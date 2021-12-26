package com.xinchen.builder;

import org.apache.http.HttpException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpExecutionAware;
import org.apache.http.client.methods.HttpRequestWrapper;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.client.cache.CachingHttpClientBuilder;
import org.apache.http.impl.execchain.ClientExecChain;

import java.io.IOException;
import java.net.URI;

/**
 * 装饰httpclient在执行时候的一些特殊操作
 *
 * see {@link CachingHttpClientBuilder#decorateMainExec(org.apache.http.impl.execchain.ClientExecChain)}
 *
 * @author Xin Chen (xinchenmelody@gmail.com)
 * @version 1.0
 * @date Created In 2021/12/26 22:45
 */
public class ConditionalCachingExec implements ClientExecChain {
    private final ClientExecChain mainExec;
    private final ClientExecChain cachingExec;

    public ConditionalCachingExec(final ClientExecChain mainExec, final ClientExecChain cachingExec) {
        this.mainExec = mainExec;
        this.cachingExec = cachingExec;
    }

    @Override
    public CloseableHttpResponse execute(
            final HttpRoute route,
            final HttpRequestWrapper request,
            final HttpClientContext clientContext,
            final HttpExecutionAware execAware) throws IOException, HttpException {
        URI uri = request.getURI();
        if ("/get".equals(uri.getPath())) {
            return cachingExec.execute(route, request, clientContext, execAware);
        } else {
            return mainExec.execute(route, request, clientContext, execAware);
        }
    }
}
