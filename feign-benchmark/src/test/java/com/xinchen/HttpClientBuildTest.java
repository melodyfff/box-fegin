package com.xinchen;

import com.xinchen.builder.MyCachingHttpClientBuilder;
import org.apache.http.client.cache.CacheResponseStatus;
import org.apache.http.client.cache.HttpCacheContext;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.cache.CacheConfig;
import org.apache.http.impl.client.cache.CachingExec;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

import java.io.IOException;

/**
 *
 * 注意： 根据header中的 Cache-Control 控制缓存
 * Cache-Control： https://developer.mozilla.org/zh-CN/docs/Web/HTTP/Headers/Cache-Control
 *
 * 注意观察{@link CachingExec}的执行过程，主要在 handleCacheHit() 方法调用中判断是否使用缓存，以请求的header中的Cache-Control为判断条件
 *
 *
 * 以下两个值必须添加：
 * Cache-Control: max-age=<seconds>
 * Cache-Control: max-stale=[=<seconds>]
 *
 * @author Xin Chen (xinchenmelody@gmail.com)
 * @version 1.0
 * @date Created In 2021/12/26 12:08
 */
public class HttpClientBuildTest {
    private static final String url = "http://httpbin.org/get";
    private static final RequestConfig requestConfig = RequestConfig.custom()
            // SocketTimeoutException: connect timed out
            .setConnectTimeout(10000)
            // SocketTimeoutException: Read timed out
            .setSocketTimeout(10000)
            .build();
    private static final HttpUriRequest request = RequestBuilder
            .create("GET")
            .setUri(url)
            // 添加缓存 - 这两个值一定要添加才会生效
            .addHeader("Cache-Control","max-age=100")
            .addHeader("Cache-Control","max-stale=100")
            .setConfig(requestConfig)
            .build();

    @Test
    public void build_single_normal(){
        CloseableHttpClient httpClient = HttpClientBuilder
                .create()
                .build();
        try (CloseableHttpResponse execute = httpClient.execute(request)){
            System.out.println(EntityUtils.toString(execute.getEntity()));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void build_loop_normal(){
        CloseableHttpClient httpClient = HttpClientBuilder
                .create()
                .build();

        long start = System.currentTimeMillis();
        for (int i = 0; i < 200; i++) {
            try (CloseableHttpResponse execute = httpClient.execute(request)){
                System.out.println(EntityUtils.toString(execute.getEntity()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Cost: "+ (System.currentTimeMillis()-start)+" ms");
    }

    @Test
    public void build_cache(){
        CacheConfig cacheConfig = CacheConfig
                .custom()
                .setMaxCacheEntries(3000)
                .setMaxObjectSize(10240)
                .build();
        PoolingHttpClientConnectionManager poolingHttpClientConnectionManager = new PoolingHttpClientConnectionManager();


        CloseableHttpClient cacheHttpClient = MyCachingHttpClientBuilder.create()
                .setCacheConfig(cacheConfig)
//                .setHttpCacheStorage(new BasicHttpCacheStorage(cacheConfig))
                .setConnectionManager(poolingHttpClientConnectionManager)
                .build();

        HttpCacheContext httpCacheContext = HttpCacheContext.create();

        long start = System.currentTimeMillis();
        for (int i = 0; i < 200; i++) {
            try (CloseableHttpResponse execute = cacheHttpClient.execute(request,httpCacheContext)){
//                System.out.println(EntityUtils.toString(execute.getEntity()));
                CacheResponseStatus responseStatus = httpCacheContext.getCacheResponseStatus();
                switch (responseStatus) {
                    case CACHE_HIT:
                        System.out.println("A response was generated from the cache with " +
                                "no requests sent upstream");
                        break;
                    case CACHE_MODULE_RESPONSE:
                        System.out.println("The response was generated directly by the " +
                                "caching module");
                        break;
                    case CACHE_MISS:
                        System.out.println("The response came from an upstream server");
                        break;
                    case VALIDATED:
                        System.out.println("The response was generated from the cache " +
                                "after validating the entry with the origin server");
                        break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Cost: "+ (System.currentTimeMillis()-start)+" ms");
    }


}
