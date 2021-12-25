# JMH Start
===================
## Resources

[官网DEMO](http://hg.openjdk.java.net/code-tools/jmh/file/tip/jmh-samples/src/main/java/org/openjdk/jmh/samples/)

[JMH应用指南](https://dunwu.github.io/javatech/test/jmh.html)

可视化：
 - [JMH Visual Chart](http://deepoove.com/jmh-visual-chart/)
 - [JMH Visualizer](https://jmh.morethan.io/)

快速创建项目
```bash
mvn archetype:generate \
 -DinteractiveMode=false \ 
 -DarchetypeGroupId=org.openjdk.jmh \ 
 -DarchetypeArtifactId=jmh-java-benchmark-archetype \
# -DarchetypeVersion=1.25 \
 -DgroupId=com.xinchen \
 -DartifactId=jmh \
 -Dversion=1.0.0-SNAPSHOT
```

测试结果输出,JMH支持多种格式的结果输出text, csv, scsv, json, latex
```bash
java -jar benchmark.jar -rf json
```

## Introduction
- **@State(Scope.Thread)**: 
  - Benchmark: 整个基准测试的生命周期，多个线程共用同一份实例对象。该类内部的@Setup @TearDown注解的方法可能会被任一个线程执行，但是只会执行一次。
  - Group:  每一个Group内部共享同一个实例，需要配合@Group @GroupThread使用。该类内部的@Setup @TearDown注解的方法可能会该Group内的任一个线程执行，但是只会执行一次。
  - Thread: 每个线程的实例都是不同的、唯一的。该类内部的@Setup @TearDown注解的方法只会被当前线程执行，而且只会执行一次。
  
-----
    
- **@Benchmark**： 标记基准测试方法，方法必须为`public`

-----

- **@BenchmarkMode({Mode.SampleTime, Mode.AverageTime})**:
    - Throughput：整体吞吐量，每秒执行了多少次调用，单位为 ops/time
    - AverageTime：用的平均时间，每次操作的平均时间，单位为 time/op
    - SampleTime：随机取样，最后输出取样结果的分布
    - SingleShotTime：只运行一次，往往同时把 Warmup 次数设为 0，用于测试冷启动时的性能
    - All：上面的所有模式都执行一次

-----

- **@OutputTimeUnit(TimeUnit.SECONDS)**:打印基准测试结果的时间单位

-----

- **@Setup、@TearDown**: @Setup类似于 junit 的@Before，而@TearDown类似于 junit 的@After
  - Trial： 每次benchmark前/后执行一次，每次benchmark会包含多轮（Iteration）
  - Iteration： 每轮Iteration 执行前/后执行一次
  - Invocation： 每次调用测试的方法前/后都执行一次，这个执行频率会很高，一般用不上。
  
-----

- **@Fork**:  用来设置启动的JVM进程数量，可用于类或者方法上。如果 fork 数是 2 的话，则 JMH 会 fork 出两个进程来进行测试。多个进程是串行的方式启动的，多个进程可以减少偶发因素对测试结果的影响。

-----

- **@Threads**: 每个进程中的测试线程，可用于类或者方法上

-----

- **@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)**:基准测试对代码预热总计5秒（迭代5次，每次1秒）

-----

- **@Measurement(iterations = 5, time = 1)**: 实际调用方法所需要配置的一些基本测试参数，可用于类或者方法上（迭代5次，每次1秒）

-----

- **@Param({"list", "iterator", "stream"})**：只能作用在字段上，参数输入，使用该注解必须定义 @State 注解