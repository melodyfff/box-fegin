package com.xinchen.builder;

import org.apache.http.impl.client.cache.CachingHttpClientBuilder;
import org.apache.http.impl.execchain.ClientExecChain;

/**
 * @author Xin Chen (xinchenmelody@gmail.com)
 * @version 1.0
 * @date Created In 2021/12/26 22:44
 */
public class MyCachingHttpClientBuilder extends CachingHttpClientBuilder {
    @Override
    protected ClientExecChain decorateMainExec(final ClientExecChain mainExec) {
        ClientExecChain cachingExec = super.decorateMainExec(mainExec);
        return new ConditionalCachingExec(mainExec, cachingExec);
    }
}
