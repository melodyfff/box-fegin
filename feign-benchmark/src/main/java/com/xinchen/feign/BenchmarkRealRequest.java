package com.xinchen.feign;

import feign.Feign;
import feign.Logger;
import feign.Response;
import feign.Retryer;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import io.netty.buffer.ByteBuf;
import io.reactivex.netty.protocol.http.server.HttpServer;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * @author Xin Chen (xinchenmelody@gmail.com)
 * @version 1.0
 * @date Created In 2021/12/25 15:48
 */
@Measurement(iterations = 5, time = 1)
@Warmup(iterations = 10, time = 1)
@Fork(3)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
public class BenchmarkRealRequest {
    private static final int SERVER_PORT = 8765;
    private HttpServer<ByteBuf, ByteBuf> server;
    private OkHttpClient client;
    private TestInterface okFeign;
    private Request queryRequest;

    @Setup
    public void setup() {

        server = HttpServer.newServer(SERVER_PORT)
                .start((request, response) -> null);
        client = new OkHttpClient();
        client.retryOnConnectionFailure();
        okFeign = Feign.builder()
                .client(new feign.okhttp.OkHttpClient(client))
                .logLevel(Logger.Level.NONE)
                .logger(new Logger.ErrorLogger())
                .retryer(new Retryer.Default())
                .target(TestInterface.class, "http://localhost:" + SERVER_PORT);
        queryRequest = new Request.Builder()
                .url("http://localhost:" + SERVER_PORT + "/?Action=GetUser&Version=2010-05-08&limit=1")
                .build();
    }

    @TearDown
    public void tearDown() throws InterruptedException {
        server.shutdown();
    }


    /**
     * How fast can we execute get commands synchronously?
     */
    @Benchmark
    public okhttp3.Response query_baseCaseUsingOkHttp() throws IOException {
        okhttp3.Response result = client.newCall(queryRequest).execute();
        result.body().close();
        return result;
    }

    /**
     * How fast can we execute get commands synchronously using Feign?
     */
    @Benchmark
    public boolean query_feignUsingOkHttp() {
        /* auto close the response */
        try (Response ignored = okFeign.query()) {
            return true;
        }
    }

}
