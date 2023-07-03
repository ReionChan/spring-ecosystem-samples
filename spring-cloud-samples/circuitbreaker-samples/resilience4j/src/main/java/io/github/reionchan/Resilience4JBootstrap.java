package io.github.reionchan;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadConfig;
import io.github.resilience4j.bulkhead.internal.FixedThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.internal.SemaphoreBulkhead;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.common.CompositeCustomizer;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.core.ContextAwareScheduledThreadPoolExecutor;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.spring6.circuitbreaker.configure.CircuitBreakerConfiguration;
import io.github.resilience4j.spring6.circuitbreaker.configure.CircuitBreakerConfigurationProperties;
import io.github.resilience4j.spring6.timelimiter.configure.TimeLimiterConfiguration;
import io.github.resilience4j.spring6.timelimiter.configure.TimeLimiterConfigurationProperties;
import io.github.resilience4j.springboot3.bulkhead.autoconfigure.BulkheadAutoConfiguration;
import io.github.resilience4j.springboot3.circuitbreaker.autoconfigure.AbstractCircuitBreakerConfigurationOnMissingBean;
import io.github.resilience4j.springboot3.circuitbreaker.autoconfigure.CircuitBreakerProperties;
import io.github.resilience4j.springboot3.timelimiter.autoconfigure.AbstractTimeLimiterConfigurationOnMissingBean;
import io.github.resilience4j.springboot3.timelimiter.autoconfigure.TimeLimiterProperties;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.circuitbreaker.resilience4j.*;
import org.springframework.cloud.client.circuitbreaker.AbstractCircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

import java.time.Duration;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * Resilience4J 熔断器实现
 *
 * <pre>
 *
 * ==========================
 * Resilience4J 熔断器的配置
 * ==========================
 *
 * 对它的配置可通过两种方式
 * 1. Java 方式
 * 2. Properties 文件配置方式
 *
 * 其中，Properties 文件配置方式优先级高于 Java 方式。
 *
 * 优先级原理：
 *
 * 使用 Java 接口 {@link Customizer} Bean 配置 {@link Resilience4JCircuitBreakerFactory}
 *
 * 通过调用后者的方法：
 *  {@link Resilience4JCircuitBreakerFactory#configureDefault(Function)}、
 *  {@link AbstractCircuitBreakerFactory#configure(Consumer, String...)}、
 *  {@link Resilience4JCircuitBreakerFactory#addCircuitBreakerCustomizer(Customizer, String...)}
 *
 * 来设置它的属性：
 *  {@link Resilience4JCircuitBreakerFactory#defaultConfiguration}、
 *  {@link AbstractCircuitBreakerFactory#configurations}、
 *  {@link Resilience4JCircuitBreakerFactory#circuitBreakerCustomizers}
 *
 * 进而影响它的创建熔断器的方法的配置：
 *  {@link Resilience4JCircuitBreakerFactory#create(String, String, ExecutorService)}
 *  {@code
 *      private Resilience4JCircuitBreaker create(String id, String groupName,
 * 			ExecutorService circuitBreakerExecutorService) {
 * 		    // 优先 AbstractCircuitBreakerFactory#configurations 找 id 的配置，
 * 		    // 没找到，则使用默认 Resilience4JCircuitBreakerFactory#defaultConfiguration 配置
 *          Resilience4JConfigBuilder.Resilience4JCircuitBreakerConfiguration defaultConfig =
 *              getConfigurations().computeIfAbsent(id, defaultConfiguration);
 *        }
 *  }
 *
 * 可以看出，采用 Java 接口 Customizer 的配置只是影响 defaultConfig 配置属性的取值，
 * 而该配置属性对生成熔断器的影响优先级最低：
 *
 *  {@link Resilience4JCircuitBreakerFactory#create(String, String, ExecutorService)}
 *  {@code
 *      private Resilience4JCircuitBreaker create(String id, String groupName,
 * 			ExecutorService circuitBreakerExecutorService) {
 * 		    // Java 接口 Customizer 配置的 defaultConfig
 * 		    Resilience4JConfigBuilder.Resilience4JCircuitBreakerConfiguration defaultConfig =
 * 		        getConfigurations().computeIfAbsent(id, defaultConfiguration);
 *
 * 		    // circuitBreakerConfig 先从 circuitBreakerRegistry 找 id 的配置，
 * 		    // 没找到，还是从 circuitBreakerRegistry 以 groupName 找配置，
 * 		    // 若还是没找到，最后才使用 defaultConfig 里的 getCircuitBreakerConfig
 * 		    CircuitBreakerConfig circuitBreakerConfig = this.circuitBreakerRegistry.getConfiguration(id)
 * 				.orElseGet(() -> this.circuitBreakerRegistry.getConfiguration(groupName)
 * 						.orElseGet(defaultConfig::getCircuitBreakerConfig));
 *
 * 		    // circuitBreakerConfig 先从 timeLimiterRegistry 找 id 的配置，
 * 		    // 没找到，还是从 timeLimiterRegistry 以 groupName 找配置，
 * 		    // 若还是没找到，最后才使用 defaultConfig 里的 getTimeLimiterConfig
 * 		    TimeLimiterConfig timeLimiterConfig = this.timeLimiterRegistry.getConfiguration(id)
 * 				.orElseGet(() -> this.timeLimiterRegistry.getConfiguration(groupName)
 * 						.orElseGet(defaultConfig::getTimeLimiterConfig));
 *
 * 		    // 配置 circuitBreakerConfig、timeLimiterConfig 来构造 Resilience4JCircuitBreaker 熔断器
 * 		    return new Resilience4JCircuitBreaker(id, groupName, circuitBreakerConfig, timeLimiterConfig,
 * 					circuitBreakerRegistry, timeLimiterRegistry, Optional.ofNullable(circuitBreakerCustomizers.get(id)),
 * 					bulkheadProvider);
 *      }
 *  }
 *
 * 而 {@link CircuitBreakerRegistry}、{@link TimeLimiterRegistry} 的配置来源于 Properties 文件配置
 * 因为这两个类型的 Bean 分别在自动装配类中定义：
 *
 *  {@link AbstractCircuitBreakerConfigurationOnMissingBean#circuitBreakerRegistry(EventConsumerRegistry,
 *      RegistryEventConsumer, CompositeCustomizer)}
 *
 *  {@link AbstractTimeLimiterConfigurationOnMissingBean#timeLimiterRegistry(TimeLimiterConfigurationProperties,
 *      EventConsumerRegistry, RegistryEventConsumer, CompositeCustomizer)}
 *
 * 不难发现，它们创建方法来源于：
 *
 *  {@link CircuitBreakerConfiguration#createCircuitBreakerRegistry(CircuitBreakerConfigurationProperties,
 *      RegistryEventConsumer, CompositeCustomizer)}
 *
 *  {@link TimeLimiterConfiguration#timeLimiterRegistry(TimeLimiterConfigurationProperties,
 *      EventConsumerRegistry, RegistryEventConsumer, CompositeCustomizer)}
 *
 * 而其中的 {@link CircuitBreakerConfigurationProperties}、{@link TimeLimiterConfigurationProperties} 类型
 * 的自动装配的 Bean 都由它们的子类 {@link CircuitBreakerProperties}、{@link TimeLimiterProperties} 来定义
 * 而这两个属性的配置来源由它们类上的注解 @ConfigurationProperties 来指定：
 *
 * {@code
 *  @ConfigurationProperties(prefix = "resilience4j.circuitbreaker")
 *  public class CircuitBreakerProperties extends CircuitBreakerConfigurationProperties {
 *  }
 * }
 *
 * {@code
 *  @ConfigurationProperties(prefix = "resilience4j.timelimiter")
 *  public class TimeLimiterProperties extends TimeLimiterConfigurationProperties {
 *  }
 * }
 *
 * =========================
 * Resilience4J Bulkhead
 * =========================
 * Bulkhead 特性是由 {@link BulkheadAutoConfiguration} 自动装配类配置，
 * 它将被调用方的服务限制在指定并发数内，调用方超过并发数的调用将被舍弃。
 * 这就好像船的隔仓板一样，被调用方的不同服务都被限制成不同并发数的调用，
 * 使得不同类型的服务占用一定的线程资源，相互隔离，不会相互抢占对方的线程资源。
 *
 * 激活 Bulkhead 使用属性配置
 *  spring.cloud.circuitbreaker.bulkhead.resilience4j.enabled=true
 * 原理：
 *  {@link Resilience4JAutoConfiguration.Resilience4jBulkheadConfiguration}
 *
 * 与熔断器类似，它也支持两种配置方式：
 * 1. Java 方式
 * 2. Properties 文件配置方式
 *
 * 隔仓有两种形式的实现方式：
 * 1. 信号量形式的 {@link SemaphoreBulkhead}
 * 2. 线程池形式的 {@link FixedThreadPoolBulkhead}
 *
 * </pre>
 *
 * @author Reion
 * @date 2023-06-28
 */
@Slf4j
@SpringBootApplication
public class Resilience4JBootstrap {

    public static final AtomicLong counter = new AtomicLong(0);

    /**
     * 定制化 Resilience4JCircuitBreakerFactory
     */
    // @formatter:off
    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> customR4JFactory() {
        return factory -> {
            // --------------------------------------
            //        所有熔断器兜底默认配置
            // --------------------------------------
            // defaultConfiguration, 创建熔断器时，没有另外指定参数时所采取的默认配置
            factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
                // 设置 4 秒的时间限制
                .timeLimiterConfig(TimeLimiterConfig.custom()
                    .timeoutDuration(Duration.ofSeconds(4)).build())
                // 熔断器均保持默认设置
                .circuitBreakerConfig(CircuitBreakerConfig.ofDefaults())
                .build());

            // executorService, 创建熔断器时，未指定执行器时所采用的默认执行器
            ContextAwareScheduledThreadPoolExecutor executor = ContextAwareScheduledThreadPoolExecutor
                    .newScheduledThreadPool().corePoolSize(Runtime.getRuntime().availableProcessors()).build();
            factory.configureExecutorService(executor);

            // --------------------------------------
            //     为多个特定 ID 名称的熔断器附加特殊配置
            // --------------------------------------
            // configurations, 该配置将会在指定的 ID 熔断器创建时替换上面的默认配置
            // 参见方法：Resilience4JCircuitBreakerFactory.create(String, String, ExecutorService)
            factory.configure(resilience4JConfigBuilder -> {
                resilience4JConfigBuilder.circuitBreakerConfig(CircuitBreakerConfig.ofDefaults())
                    // 指定 slowA、groupA、slowC、groupC 时间限制设置为 5 秒，取代上面默认的 4 秒
                    .timeLimiterConfig(TimeLimiterConfig.custom()
                        .timeoutDuration(Duration.ofSeconds(5)).build());
            },"slowA", "groupA", "slowC", "groupC");

            // --------------------------------------
            //     为多个特定 ID 名称的熔断器附加定制器
            // --------------------------------------
            // circuitBreakerCustomizers, 该定制器将会在指定的 ID 熔断器执行 run 任务之前执行定制化处理
            // 参见方法：Resilience4JCircuitBreaker#run(Supplier<T>, Function<Throwable, T>)
            Customizer<io.github.resilience4j.circuitbreaker.CircuitBreaker> onceCustomizerById
                    // Resilience4JCircuitBreakerFactory 创建的熔断器 Resilience4JCircuitBreaker 实例对象
                    // 会在每次执行 run 之前向当前的 io.github.resilience4j.circuitbreaker.CircuitBreaker 实例
                    // 重复调用定制化 Customizer<CircuitBreaker>，但是像下方注册错误日志记录器，对同一个实例，没必要重复注册
                    // 所以，改用 Customizer.once(Customizer) 生成包装定制器，它能使得同一个 CircuitBreaker 熔断器实例
                    // 执行多次 run 时，都只执行一次被包装的 Customizer，使得下方的日志记录处理只被登记一次。
                    // （如果被登记多次，相当于有多个相同的异常日志记录器，这将导致一次异常，被多个异常日志记录器重复打印输出多次）
                    // 参考方法：Resilience4JCircuitBreaker.run
                = Customizer.once(circuitBreaker -> circuitBreaker.getEventPublisher()
                    .onError(event -> log.error("Error: {}", event.getThrowable().getMessage())),
                    // 注意：
                    //    键值映射只使用 CircuitBreaker::getName，只会提取熔断器的 id 来匹配是否已经执行过。
                    //    如果此设置为组标识，那么所有在这个组里的熔断器只有一个熔断器获得一次执行机会。
                    io.github.resilience4j.circuitbreaker.CircuitBreaker::getName);

            // 向 id 为 slowB 的熔断器中注册【id 匹配型】的执行错误时的日志记录，这里的可变参数只能填熔断器的 id，不能填 groupName
            // 原因：Resilience4JCircuitBreakerFactory.create(String, String, ExecutorService)
            //      创建 Resilience4JCircuitBreaker 实例的构造参数只通过 id 来匹配熔断器定制器
            //      Optional.ofNullable(circuitBreakerCustomizers.get(id))
            factory.addCircuitBreakerCustomizer(onceCustomizerById, "slowB", "slowC");

            // --------------------------------------
            //     专门用来观察熔断状态变化的熔断器配置
            // --------------------------------------
            factory.configure(resilience4JConfigBuilder -> {
                resilience4JConfigBuilder.circuitBreakerConfig(CircuitBreakerConfig.custom()
                    // 滑动窗口大小 10，至少调用次数 5，基于次数的滑动窗口类型
                    .slidingWindow(10, 5, CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                    // 错误率 ≥ 50% 发生熔断
                    .failureRateThreshold(50)
                    // 熔断后等待 5 秒后开始恢复
                    .waitDurationInOpenState(Duration.ofSeconds(5))
                    // 设置熔断结束后自动转入半断开状态（false 时，转变过程由被动调用驱动，true 时，设置定时器自动转变）
                    .automaticTransitionFromOpenToHalfOpenEnabled(true)
                    // 半断开状态允许进行 5 次试探性调用，如果错误率 ＜ failureRateThreshold，即上面配置的 50%，认为下游恢复正常
                    // 此时，断路器关闭，恢复允许调用下游服务
                    .permittedNumberOfCallsInHalfOpenState(5).build())
                // 等待 1 秒，超时取消任务执行
                .timeLimiterConfig(TimeLimiterConfig.custom()
                    .cancelRunningFuture(true)
                    .timeoutDuration(Duration.ofSeconds(2)).build());
            },"cb4StateShift");

            // 给 cb4StateShift 断路器注册状态变化事件的消费者
            Customizer<io.github.resilience4j.circuitbreaker.CircuitBreaker> onceCustomizer4StateShift
                = Customizer.once(circuitBreaker -> circuitBreaker.getEventPublisher()
                    .onStateTransition(event -> {
                        log.info("\n\n{} --变更--> {}\n",
                            event.getStateTransition().getFromState().name(),
                            event.getStateTransition().getToState().name());
                        counter.set(0);
                    }),
                    io.github.resilience4j.circuitbreaker.CircuitBreaker::getName);
            factory.addCircuitBreakerCustomizer(onceCustomizer4StateShift, "cb4StateShift");
        };
    }
    // @formatter:on

    /**
     * 定制化 Resilience4jBulkheadProvider
     */
    // @formatter:off
    @Bean
    public Customizer<Resilience4jBulkheadProvider> customR4JProvider() {
        return bulkheadProvider -> {
            bulkheadProvider.configureDefault(id -> new Resilience4jBulkheadConfigurationBuilder()
                // 基于信号量实现的 SemaphoreBulkhead 配置
                .bulkheadConfig(BulkheadConfig.custom()
                    // 调用线程尝试进入 bulkhead 最大等待时间，超时线程将被打断
                    .maxWaitDuration(Duration.ofSeconds(0))
                    // 最大并发调用数（用来控制并发数）
                    .maxConcurrentCalls(2).build())
                // 基于线程池实现的 FixedThreadPoolBulkhead 配置
                .threadPoolBulkheadConfig(ThreadPoolBulkheadConfig.custom()
                    // bulkhead 线程池核心线程数
                    .coreThreadPoolSize(2)
                    // bulkhead 线程池最大线程数 （用来控制并发数）
                    .maxThreadPoolSize(2)
                    // bulkhead 等待队列容量
                    .queueCapacity(2)
                    // bulkhead 线程池过载后自定义处理器
                    .rejectedExecutionHandler((run, executor) -> {
                        log.warn("线程池队列已满，拒绝！");
                        throw new RejectedExecutionException("Task " + run.toString() +
                                " rejected from " +
                                executor.toString());
                    }).build())
                .build());
            // 基于 Semaphore 实现的 Bulkhead 定制
            Customizer<Bulkhead> semaphoreBulkheadCustomizer
                    = Customizer.once(bulkhead -> bulkhead.getEventPublisher()
                            .onCallRejected(event -> {
                                log.warn("{} 放弃任务执行 {}",
                                        event.getBulkheadName(),
                                        event.getEventType());
                            }),
                    Bulkhead::getName);
            bulkheadProvider.addBulkheadCustomizer(semaphoreBulkheadCustomizer, "semaphoreBulkhead");

            // 基于 ThreadPool 实现的 Bulkhead 定制
            Customizer<ThreadPoolBulkhead> threadPoolBulkheadCustomizer
                    = Customizer.once(bulkhead -> bulkhead.getEventPublisher()
                            .onCallRejected(event -> {
                                log.warn("{} 放弃任务执行 {}",
                                        event.getBulkheadName(),
                                        event.getEventType());
                            }),
                    ThreadPoolBulkhead::getName);
            bulkheadProvider.addThreadPoolBulkheadCustomizer(threadPoolBulkheadCustomizer, "threadPoolBulkhead");
        };
    }
    // @formatter:on

    public static void main(String[] args) throws InterruptedException {
        ConfigurableApplicationContext context = SpringApplication.run(Resilience4JBootstrap.class, args);

        Resilience4JCircuitBreakerFactory factory = context.getBean(Resilience4JCircuitBreakerFactory.class);
        log.info("=== 自动装配默认 CircuitBreakerFactory: {}", factory.getClass().getSimpleName());

        // 配置优先级测试
        configCircuitBreakerFactory(factory);

        // 熔断器关闭、开启、半开启保护机制测试
        circuitBreakerStateShift(factory);

        // Bulkhead 隔仓保护机制测试
        semaphoreAndThreadPoolBulkhead(factory);
    }

    /**
     * Java 与 yaml 文件配置优先级测试
     */
    private static void configCircuitBreakerFactory(Resilience4JCircuitBreakerFactory factory) {

        // 降级执行补偿方法
        Function<Throwable, String> fallback = throwable -> throwable.getMessage();

        // 时间限制：application.yaml default (默认配置 2 秒) > Customizer（默认配置 4 秒），故：设置 2 秒
        // 熔断定制：无
        CircuitBreaker cbDefaultName = factory.create("default");
        log.info("【default】execute 1 sec limit 2 sec, result: {}", cbDefaultName.run(() -> slowMock(1), fallback));
        log.info("【default】execute 3 sec limit 2 sec, result: {}", cbDefaultName.run(() -> slowMock(3), fallback));

        // 时间限制：application.yaml 没找到名称为 noFoundName (注意：没找到不会使用 yaml 文件中名称为 default 的配置)
        //         Customizer（默认配置 4 秒），
        //         故：设置 4 秒
        // 熔断定制：无
        CircuitBreaker cbNoFoundName = factory.create("noFoundName");
        log.info("【noFoundName】execute 1 sec limit 4 sec, result: {}", cbNoFoundName.run(() -> slowMock(1), fallback));
        log.info("【noFoundName】execute 5 sec limit 4 sec, result: {}", cbNoFoundName.run(() -> slowMock(5), fallback));

        // 时间限制：Customizer slowA（特定配置 5 秒），故：设置 5 秒
        // 熔断定制：无
        CircuitBreaker cbSlowA = factory.create("slowA");
        log.info("【slowA】execute 3 sec limit 5 sec, result: {}", cbSlowA.run(() -> slowMock(3), fallback));
        log.info("【slowA】execute 6 sec limit 5 sec, result: {}", cbSlowA.run(() -> slowMock(6), fallback));

        // 时间限制：application.yaml slowB (特定配置 5 秒) > Customizer（默认配置 4 秒），故：设置 5 秒
        // 熔断定制：Customizer slowB（特定配置 异常日志记录），故：超时会有日志输出
        CircuitBreaker cbSlowB = factory.create("slowB");
        log.info("【slowB】execute 4 sec limit 5 sec, result: {}", cbSlowB.run(() -> slowMock(4), fallback));
        log.info("【slowB】execute 6 sec limit 5 sec, result: {}", cbSlowB.run(() -> slowMock(6), fallback));

        // 创建实例名称 slowC 组名称 groupC 的熔断器实例
        // 时间限制：
        //  application.yaml slowC (特定配置 3 秒) >
        //      application.yaml groupC (特定组配置 4 秒) >
        //          Customizer slowC（特定配置 5 秒）>
        //              Customizer groupC（特定组配置 5 秒）
        //  故：设置 3 秒
        // 熔断定制：Customizer slowC（特定配置 异常日志记录），故：超时会有日志输出
        CircuitBreaker cbSlowC = factory.create("slowC", "groupC");
        log.info("【slowC-groupC】execute 2 sec limit 3, result: {}", cbSlowC.run(() -> slowMock(2), fallback));
        log.info("【slowC-groupC】execute 4 sec limit 3, result: {}", cbSlowC.run(() -> slowMock(8), fallback));
    }

    /**
     * 熔断器几种状态切换测试
     */
    private static void circuitBreakerStateShift(Resilience4JCircuitBreakerFactory factory) throws InterruptedException {
        // 创建 2 个线程的执行器
        ExecutorService executorService = mockFixConcurrentCapability(2);
        Random random = new Random();
        // 创建具备状态监听的熔断器实例
        CircuitBreaker cb4StateShift = factory.create("cb4StateShift");
        int[] exitCounter = {0};
        while (exitCounter[0] < 30) {
            Thread.sleep(random.nextLong(200));
            // 每间隔 200 毫秒向熔断器提交一个任务，该任务是将 taskMock 交给 2 个线程的执行器执行
            cb4StateShift.run(() -> {
                executorService.submit(() -> taskMock(random.nextLong(2000) + 200));
                return "done!";
            }, throwable -> {
                if (throwable instanceof CallNotPermittedException cnp) {
                    log.warn("【OPEN】熔断中，5s 后待恢复...", counter.incrementAndGet());
                } else {
                    log.error("【下游第 {} 次异常】：{}", counter.incrementAndGet(), throwable.getMessage());
                }
                try {
                    Thread.sleep(1000);
                    exitCounter[0]++;
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                return "fallback!";
            });
        }
    }

    /**
     * 信号量、线程池，这两种形式的隔仓测试
     */
    private static void semaphoreAndThreadPoolBulkhead(Resilience4JCircuitBreakerFactory factory) {
        log.info("=== SemaphoreBulkhead 隔仓测试 ===");
        Random random = new Random();

        // 信号量形式的隔仓
        CircuitBreaker semaphoreBulkhead = factory.create("semaphoreBulkhead");
        IntStream.range(1, 10).parallel().forEach(n ->
                semaphoreBulkhead.run(() -> {
                    log.info("执行第 {} 次任务", n);
                    try {
                        Thread.sleep(random.nextLong(1000));
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    return Void.class;
                }, throwable -> {
                    log.error("第 {} 次任务 fallback，异常：{}", n, throwable.getClass().getSimpleName());
                    return Void.class;
                })
        );

        log.info("=== FixedThreadPoolBulkhead 隔仓测试 ===");
        // 线程池形式的隔仓
        CircuitBreaker threadPoolBulkhead = factory.create("threadPoolBulkhead");
        IntStream.range(1, 10).parallel().forEach(n ->
                threadPoolBulkhead.run(() -> {
                    log.info("执行第 {} 次任务", n);
                    try {
                        Thread.sleep(random.nextLong(1000));
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    return Void.class;
                }, throwable -> {
                    log.error("第 {} 次任务 fallback，异常：{}", n, throwable.getClass().getSimpleName());
                    return Void.class;
                })
        );
    }

    /**
     * 模仿较慢的任务操作
     */
    public static String slowMock(int seconds) {
        int sec = seconds > 0 ? seconds : 1;
        try {
            Thread.sleep(sec * 1000);
            return "success";
        } catch (InterruptedException e) {
            return "InterruptedException";
        }
    }

    /**
     * 模仿正常任务操作
     */
    public static void taskMock(long millis) {
        long mi = millis > 0 ? millis : 100;
        try {
            Thread.sleep(mi);
            log.info("【下游】：正常完毕！");
        } catch (InterruptedException e) {
        }
    }

    /**
     * 模仿固定并发能力的执行器
     */
    public static ExecutorService mockFixConcurrentCapability(int num) {
        int n = num > 0 ? num : Runtime.getRuntime().availableProcessors();
        return new ThreadPoolExecutor(n, n,
                0L, TimeUnit.MILLISECONDS,
                new SynchronousQueue<>(), new RejectedExecutionHandler() {
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                throw new RejectedExecutionException("超过处理能力拒绝执行！");
            }
        });
    }
}
