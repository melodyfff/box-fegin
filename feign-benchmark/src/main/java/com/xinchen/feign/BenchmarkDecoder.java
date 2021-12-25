package com.xinchen.feign;

import feign.Request;
import feign.Response;
import feign.Util;
import feign.codec.Decoder;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonIteratorDecoder;
import feign.stream.StreamDecoder;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * @author Xin Chen (xinchenmelody@gmail.com)
 * @version 1.0
 * @date Created In 2021/12/25 2:22
 */
@State(Scope.Thread)
public class BenchmarkDecoder {
    @Param({"list", "iterator", "stream"})
    private String api;

    @Param({"10", "100"})
    private String size;

    private Response response;

    private Decoder decoder;
    private Type type;

    @Benchmark
    @Warmup(iterations = 5, time = 1)
    @Measurement(iterations = 10, time = 1)
    @Fork(3)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void decode() throws Exception {
        fetch(decoder.decode(response, type));
    }

    @SuppressWarnings("unchecked")
    private void fetch(Object o) {
        Iterator<Car> cars;

        if (o instanceof Collection) {
            cars = ((Collection<Car>) o).iterator();
        } else if (o instanceof Stream) {
            cars = ((Stream<Car>) o).iterator();
        } else {
            cars = (Iterator<Car>) o;
        }

        while (cars.hasNext()) {
            cars.next();
        }
    }

    @SuppressWarnings("deprecation")
    @Setup(Level.Invocation)
    public void buildResponse() {
        response = Response.builder()
                .status(200)
                .reason("OK")
                .request(Request.create(Request.HttpMethod.GET, "/", Collections.emptyMap(), null, Util.UTF_8))
                .headers(Collections.emptyMap())
                .body(carsJson(Integer.parseInt(size)), Util.UTF_8)
                .build();
    }

    @Setup(Level.Trial)
    public void buildDecoder() {
        switch (api) {
            case "list":
                decoder = new JacksonDecoder();
                type = new TypeReference<List<Car>>() {}.getType();
                break;
            case "iterator":
                decoder = JacksonIteratorDecoder.create();
                type = new TypeReference<Iterator<Car>>() {}.getType();
                break;
            case "stream":
                decoder = StreamDecoder.create(JacksonIteratorDecoder.create());
                type = new TypeReference<Stream<Car>>() {}.getType();
                break;
            default:
                throw new IllegalStateException("Unknown api: " + api);
        }
    }


    private String carsJson(int count) {
        String car = "{\"name\":\"c4\",\"manufacturer\":\"Citroën\"}";
        StringBuilder builder = new StringBuilder("[");
        builder.append(car);
        for (int i = 1; i < count; i++) {
            builder.append(",").append(car);
        }
        return builder.append("]").toString();
    }

    static class Car {
        public String name;
        public String manufacturer;
    }
}
