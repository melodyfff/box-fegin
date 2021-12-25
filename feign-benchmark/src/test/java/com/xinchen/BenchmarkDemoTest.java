package com.xinchen;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Xin Chen (xinchenmelody@gmail.com)
 * @version 1.0
 * @date Created In 2021/12/25 12:57
 */
@State(Scope.Thread)
public class BenchmarkDemoTest {


    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @Fork(2)
    @Threads(2)
    @Warmup(iterations = 3, time = 1,timeUnit = TimeUnit.SECONDS)
    @Measurement(iterations = 3, time = 2)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void testMethod(TestClass testClass){
        testClass.getInteger().incrementAndGet();
    }

    @State(Scope.Benchmark)
    public static class TestClass{
        private AtomicInteger integer;

        @Setup(Level.Trial)
        public void setup(){
            integer = new AtomicInteger();
        }

        public AtomicInteger getInteger(){
            return integer;
        }
    }
}
