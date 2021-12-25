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
import feign.hystrix.FallbackFactory;
import feign.hystrix.HystrixFeign;
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
 *
 * Hystrix完整配置列表 : https://www.cnblogs.com/throwable/p/11961016.html#%E5%89%8D%E6%8F%90
 * Hystrix wiki config: https://github.com/Netflix/Hystrix/wiki/Configuration
 *
 * @author Xin Chen (xinchenmelody@gmail.com)
 * @version 1.0
 * @date Created In 2021/12/25 16:24
 */
@State(Scope.Thread)
public class BenchmarkHystrix {

    private static final FallbackFactory<HystrixTestInterface> fallbackFactory = new FallbackFactory<HystrixTestInterface>() {
        @Override
        public HystrixTestInterface create(Throwable cause) {
            // 这里直接抛出异常
            throw new RuntimeException(cause);
        }
    };

    private static final int REQUEST_TIME_OUT = 2000;

    HystrixTestInterface api;

    // python -m httpbin.core --port 9000
    @Param({"http://localhost:9000","http://httpbin.org"})
    private String targetUtl;

    @Param({"query"})
    private String apis;

    @Param({"10"})
    private String time;

    @Setup
    public void setUp(){
        api = HystrixFeign
                .builder()
                // 用于控制hystrix command的属性，包括从静态配置或者注解中读取配置，它是预先解析的（不会每次执行请求时执行）
                .setterFactory(((target, method) -> {
                    String groupKey = target.name();
                    String commandKey = Feign.configKey(target.type(), method);
                    return HystrixCommand.Setter
                            // HystrixCommandGroupKey是用于对Hystrix命令进行分组，分组之后便于统计展示于仪表盘、上传报告和预警等等，也就是说，HystrixCommandGroupKey是Hystrix内部进行度量统计时候的分组标识，数据上报和统计的最小维度就是分组的KEY。
                            .withGroupKey(HystrixCommandGroupKey.Factory.asKey(groupKey))
                            // HystrixCommandKey是Hystrix命令的唯一标识，准确来说是HystrixCommand实例或者HystrixObservableCommand实例的唯一标识
                            .andCommandKey(HystrixCommandKey.Factory.asKey(commandKey))
                            // HystrixThreadPoolKey主要标识用于监控、度量和缓存等等作用的HystrixThreadPool实例
                            // 一个HystrixCommand会和一个独立的HystrixThreadPool实例关联，也就是说一类HystrixCommand总是在同一个HystrixThreadPool实例中执行。
                            // 如果不显式配置HystrixThreadPoolKey，那么会使用HystrixCommandGroupKey的值去配置HystrixThreadPoolKey。
//                            .andThreadPoolKey(HystrixThreadPoolKey.Factory.asKey())
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
                                    // 隔离策略 : 默认 THREAD
                                    //    默认全局配置: hystrix.command.default.execution.isolation.strategy=THREAD
                                    //    实例配置: hystrix.command.CustomCommand.execution.isolation.strategy=THREAD
                                    .withExecutionIsolationStrategy(HystrixCommandProperties.ExecutionIsolationStrategy.THREAD)
                                    // 是否允许运行执行超时，默认 TRUE
                                    //    默认全局配置: hystrix.command.default.execution.timeout.enabled=true
                                    //    实例配置: hystrix.command.CustomCommand.execution.timeout.enabled=true
                                    .withExecutionTimeoutEnabled(true)
                                    // 超时时间，默认 1000ms
                                    //    默认全局配置: hystrix.command.default.execution.isolation.thread.timeoutInMilliseconds=1000
                                    //    实例配置: hystrix.command.CustomCommand.execution.isolation.thread.timeoutInMilliseconds=1000
                                    .withExecutionTimeoutInMilliseconds(REQUEST_TIME_OUT)
                                    // 超时是否中断，默认 true
                                    //    默认全局配置: hystrix.command.default.execution.isolation.thread.interruptOnTimeout=true
                                    //    实例配置: hystrix.command.CustomCommand.execution.isolation.thread.interruptOnTimeout=true
                                    .withExecutionIsolationThreadInterruptOnTimeout(true)
                                    // 取消是否中断, 默认 false
                                    //    默认全局配置: hystrix.command.default.execution.isolation.thread.interruptOnCancel=false
                                    //    实例配置: hystrix.command.CustomCommand.execution.isolation.thread.interruptOnCancel=false
                                    .withExecutionIsolationThreadInterruptOnFutureCancel(false)
                                    // 是否开启降级, 默认 true
                                    //    默认全局配置: hystrix.command.default.fallback.enabled=true
                                    //    实例配置: hystrix.command.CustomCommand.fallback.enabled=true
                                    .withFallbackEnabled(true)
                                    // 是否启用请求日志, 默认true
                                    .withRequestLogEnabled(true)
                            );
                }))
                .logLevel(Logger.Level.NONE)
                // connectTimeout 10S , readTimeout 60s
                .options(new Request.Options(10L, TimeUnit.SECONDS,60,TimeUnit.SECONDS,false))
                .logger(new Logger.ErrorLogger())
                .retryer(Retryer.NEVER_RETRY)
                .target(HystrixTestInterface.class, targetUtl,fallbackFactory);
    }

    @Benchmark
    @Warmup(iterations = 5, time = 1)
    @Measurement(iterations = 10, time = 1)
    @Fork(3)
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void run(){
        for (int i = 0; i < Integer.parseInt(time); i++) {
            api.query().execute();
        }
    }


    @Headers("Accept: application/json")
    interface HystrixTestInterface {
        @RequestLine("GET /get?Action=GetUser&Version=2010-05-08&limit=1")
        HystrixCommand<String> query();
    }
}
