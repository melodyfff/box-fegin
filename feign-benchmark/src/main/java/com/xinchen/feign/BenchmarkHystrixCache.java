package com.xinchen.feign;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixThreadPoolProperties;
import feign.Feign;
import feign.Headers;
import feign.Logger;
import feign.Request;
import feign.RequestLine;
import feign.Retryer;
import feign.httpclient.ApacheHttpClient;
import feign.hystrix.FallbackFactory;
import feign.hystrix.HystrixFeign;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.cache.CacheConfig;
import org.apache.http.impl.client.cache.CachingHttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

/**
 * @author Xin Chen (xinchenmelody@gmail.com)
 * @version 1.0
 * @date Created In 2021/12/26 10:34
 */
@State(Scope.Thread)
public class BenchmarkHystrixCache {

    private static final int REQUEST_TIME_OUT = 5000;

    private static final FallbackFactory<HystrixTestCacheInterface> fallbackFactory = new FallbackFactory<HystrixTestCacheInterface>() {
        @Override
        public HystrixTestCacheInterface create(Throwable cause) {
            // 这里直接抛出异常
            throw new RuntimeException(cause);
        }
    };

    private HystrixTestCacheInterface feignClient;

    // python -m httpbin.core --port 9000
    @Param({"http://httpbin.org"})
    private String targetUtl;

    @Param({"httpClient","cacheHttpClient","cacheableApacheHttpClient"})
    private String client;

    @Param({"10"})
    private String times;

    @Setup
    public void setUp(){
        CacheConfig cacheConfig = CacheConfig
                .custom()
                .setMaxCacheEntries(3000)
                .setMaxObjectSize(10240)
                .build();
        PoolingHttpClientConnectionManager poolingHttpClientConnectionManager = new PoolingHttpClientConnectionManager();


        CloseableHttpClient cacheHttpClient = CachingHttpClientBuilder.create()
                .setCacheConfig(cacheConfig)
                .setConnectionManager(poolingHttpClientConnectionManager)
                .build();

        CloseableHttpClient httpClient = HttpClientBuilder
                .create()
                .build();

        final ApacheHttpClient orgHttpClient = new ApacheHttpClient(httpClient);
        final ApacheHttpClient cachedHttpClient = new ApacheHttpClient(cacheHttpClient);
        final CacheableApacheHttpClient cacheableApacheHttpClient = new CacheableApacheHttpClient(httpClient);

        feignClient = HystrixFeign
                .builder()
                .client( client.endsWith("httpClient")? orgHttpClient:
                                client.endsWith("cacheHttpClient")? cachedHttpClient:cacheableApacheHttpClient
                        )
                .options(new Request.Options(10L, TimeUnit.SECONDS, 60L, TimeUnit.SECONDS, true))
                .setterFactory(((target, method) -> {
                    String groupKey = target.name();
                    String commandKey = Feign.configKey(target.type(), method);
                    return HystrixCommand.Setter
                            .withGroupKey(HystrixCommandGroupKey.Factory.asKey(groupKey))
                            .andCommandKey(HystrixCommandKey.Factory.asKey(commandKey))
                            .andThreadPoolPropertiesDefaults(HystrixThreadPoolProperties.Setter()
                                    // 核心线程数, 默认10
                                    .withCoreSize(10)
                                    // 最大线程数, 默认10
                                    .withMaximumSize(10)
                                    // 最大任务队列容量, 默认 -1 此属性配置为-1时使用的是SynchronousQueue，配置为大于1的整数时使用的是LinkedBlockingQueue
                                    .withMaxQueueSize(-1)
                                    // 任务拒绝的任务队列阈值, 默认5 ，当maxQueueSize配置为-1的时候，此配置项不生效
                                    .withQueueSizeRejectionThreshold(5)
                                    // 非核心线程存活时间, 默认 1分钟，当allowMaximumSizeToDivergeFromCoreSize为true并且maximumSize大于coreSize时此配置才生效
                                    .withKeepAliveTimeMinutes(1)
                                    // 是否允许最大线程数生效,默认 false
                                    .withAllowMaximumSizeToDivergeFromCoreSize(false)
                            )
                            .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
                                    .withExecutionIsolationStrategy(HystrixCommandProperties.ExecutionIsolationStrategy.THREAD)
                                    .withExecutionTimeoutInMilliseconds(REQUEST_TIME_OUT)
                            );
                }))
                .retryer(Retryer.NEVER_RETRY)
                .logLevel(Logger.Level.NONE)
                .logger(new Logger.ErrorLogger())
                .target(HystrixTestCacheInterface.class, targetUtl, fallbackFactory);
    }

    @Benchmark
    @Warmup(iterations = 5, time = 1)
    @Measurement(iterations = 10, time = 1)
    @Fork(3)
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void call(){
        for (int i = 0; i < Integer.parseInt(times); i++) {
            feignClient.query().execute();
        }
    }



    @Headers("Accept: application/json")
    interface HystrixTestCacheInterface {
        @RequestLine("GET /get?Action=GetUser&Version=2010-05-08&limit=1")
        HystrixCommand<String> query();
    }
}
