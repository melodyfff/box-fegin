package com.xinchen.feign;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import feign.Client;
import feign.Request;
import feign.Response;
import feign.Util;
import feign.httpclient.ApacheHttpClient;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.Configurable;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static feign.Util.UTF_8;

/**
 * 跳过HTTPCLIENT执行，缓存第一次的结果数据，新建{@link Response}返回
 *
 * see {@link ApacheHttpClient}
 *
 * @author Xin Chen (xinchenmelody@gmail.com)
 * @version 1.0
 * @date Created In 2021/12/26 15:55
 */
public class CacheableApacheHttpClient implements Client {
    private static final String ACCEPT_HEADER_NAME = "Accept";

    private final HttpClient client;
    private final Cache<String,CacheMeta> cachedMap = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(1, TimeUnit.SECONDS).build();



    public CacheableApacheHttpClient() {
        this(HttpClientBuilder.create().build());
    }

    public CacheableApacheHttpClient(HttpClient client) {
        this.client = client;
    }

    @Override
    public Response execute(Request request, Request.Options options) throws IOException {
        HttpUriRequest httpUriRequest;
        try {
            httpUriRequest = toHttpUriRequest(request, options);
        } catch (URISyntaxException e) {
            throw new IOException("URL '" + request.url() + "' couldn't be parsed into a URI", e);
        }

        // 从缓存中获取，这里直接跳过client.execute
        if (null!=cachedMap.getIfPresent(cacheKeyFrom(request))){
            final CacheMeta cacheMeta = cachedMap.getIfPresent(cacheKeyFrom(request));
            return Response.builder()
                    .status(cacheMeta.statusCode)
                    .reason(cacheMeta.reason)
                    .headers(cacheMeta.headers)
                    .request(request)
                    .body(toFeignBody(cacheMeta))
                    .build();
        }

        HttpResponse httpResponse = client.execute(httpUriRequest);
        return toFeignResponse(httpResponse, request);
    }

    HttpUriRequest toHttpUriRequest(Request request, Request.Options options)
            throws URISyntaxException {
        RequestBuilder requestBuilder = RequestBuilder.create(request.httpMethod().name());

        // per request timeouts
        RequestConfig requestConfig =
                (client instanceof Configurable ? RequestConfig.copy(((Configurable) client).getConfig())
                        : RequestConfig.custom())
                        .setConnectTimeout(options.connectTimeoutMillis())
                        .setSocketTimeout(options.readTimeoutMillis())
                        .build();
        requestBuilder.setConfig(requestConfig);

        URI uri = new URIBuilder(request.url()).build();

        requestBuilder.setUri(uri.getScheme() + "://" + uri.getAuthority() + uri.getRawPath());

        // request query params
        List<NameValuePair> queryParams =
                URLEncodedUtils.parse(uri, requestBuilder.getCharset());
        for (NameValuePair queryParam : queryParams) {
            requestBuilder.addParameter(queryParam);
        }

        // request headers
        boolean hasAcceptHeader = false;
        for (Map.Entry<String, Collection<String>> headerEntry : request.headers().entrySet()) {
            String headerName = headerEntry.getKey();
            if (headerName.equalsIgnoreCase(ACCEPT_HEADER_NAME)) {
                hasAcceptHeader = true;
            }

            if (headerName.equalsIgnoreCase(Util.CONTENT_LENGTH)) {
                // The 'Content-Length' header is always set by the Apache client and it
                // doesn't like us to set it as well.
                continue;
            }

            for (String headerValue : headerEntry.getValue()) {
                requestBuilder.addHeader(headerName, headerValue);
            }
        }
        // some servers choke on the default accept string, so we'll set it to anything
        if (!hasAcceptHeader) {
            requestBuilder.addHeader(ACCEPT_HEADER_NAME, "*/*");
        }

        // request body
        if (request.body() != null) {
            HttpEntity entity = null;
            if (request.charset() != null) {
                ContentType contentType = getContentType(request);
                String content = new String(request.body(), request.charset());
                entity = new StringEntity(content, contentType);
            } else {
                entity = new ByteArrayEntity(request.body());
            }

            requestBuilder.setEntity(entity);
        } else {
            requestBuilder.setEntity(new ByteArrayEntity(new byte[0]));
        }

        return requestBuilder.build();
    }

    private ContentType getContentType(Request request) {
        ContentType contentType = null;
        for (Map.Entry<String, Collection<String>> entry : request.headers().entrySet()){
            if (entry.getKey().equalsIgnoreCase("Content-Type")) {
                Collection<String> values = entry.getValue();
                if (values != null && !values.isEmpty()) {
                    contentType = ContentType.parse(values.iterator().next());
                    if (contentType.getCharset() == null) {
                        contentType = contentType.withCharset(request.charset());
                    }
                    break;
                }
            }
        }
        return contentType;
    }

    Response toFeignResponse(HttpResponse httpResponse, Request request) throws IOException {
        StatusLine statusLine = httpResponse.getStatusLine();
        int statusCode = statusLine.getStatusCode();

        String reason = statusLine.getReasonPhrase();

        Map<String, Collection<String>> headers = new HashMap<String, Collection<String>>();
        for (Header header : httpResponse.getAllHeaders()) {
            String name = header.getName();
            String value = header.getValue();

            Collection<String> headerValues = headers.get(name);
            if (headerValues == null) {
                headerValues = new ArrayList<String>();
                headers.put(name, headerValues);
            }
            headerValues.add(value);
        }

        final HttpEntity entity = httpResponse.getEntity();
        Integer length =  entity.getContentLength() >= 0 && entity.getContentLength() <= Integer.MAX_VALUE
                ? (int) entity.getContentLength()
                : null;

        final byte[] bytes = EntityUtils.toByteArray(entity);
        EntityUtils.consume(entity);

        // cache
        CacheMeta cacheMeta = new CacheMeta(statusCode, reason, headers, length, bytes);
        cachedMap.put(cacheKeyFrom(request), cacheMeta);

        return Response.builder()
                .status(statusCode)
                .reason(reason)
                .headers(headers)
                .request(request)
                .body(toFeignBody(cacheMeta))
                .build();
    }

    Response.Body toFeignBody(CacheMeta cacheMeta) throws IOException {

        return new Response.Body() {
            @Override
            public Integer length() {
                return cacheMeta.length;
            }

            @Override
            public boolean isRepeatable() {
                return true;
            }

            @Override
            public InputStream asInputStream() throws IOException {
                return  new ByteArrayInputStream(cacheMeta.body);
            }

            @SuppressWarnings("deprecation")
            @Override
            public Reader asReader() throws IOException {
                return new InputStreamReader(asInputStream(), UTF_8);
            }

            @Override
            public Reader asReader(Charset charset) throws IOException {
                Util.checkNotNull(charset, "charset should not be null");
                return new InputStreamReader(asInputStream(), charset);
            }

            @Override
            public void close() throws IOException {
                // ignore
            }
        };
    }

    private String cacheKeyFrom(Request request) {
        return request.url();
    }

    static class CacheMeta {
        int statusCode;
        String reason;
        Map<String, Collection<String>> headers;
        Integer length;
        byte[] body;

        public CacheMeta(int statusCode, String reason, Map<String, Collection<String>> headers,  Integer length, byte[] body) {
            this.statusCode = statusCode;
            this.reason = reason;
            this.headers = headers;
            this.length = length;
            this.body = body;
        }
    }

}

