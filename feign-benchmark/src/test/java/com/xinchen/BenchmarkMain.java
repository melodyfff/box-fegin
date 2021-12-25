package com.xinchen;

import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * @author Xin Chen (xinchenmelody@gmail.com)
 * @version 1.0
 * @date Created In 2021/12/25 13:46
 */
public class BenchmarkMain {
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(BenchmarkDemoTest.class.getSimpleName())
                .result("jmh_result.json")
                .resultFormat(ResultFormatType.JSON)
                .build();
        // run options
        new Runner(opt).run();
    }
}
