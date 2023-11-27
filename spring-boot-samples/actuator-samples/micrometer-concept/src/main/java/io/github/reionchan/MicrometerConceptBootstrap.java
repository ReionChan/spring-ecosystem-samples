package io.github.reionchan;

import io.github.reionchan.demo.DemoTarget;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.config.MeterRegistryConfig;
import io.micrometer.core.instrument.logging.LoggingMeterRegistry;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.core.instrument.observation.MeterObservationHandler;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.*;
import io.micrometer.observation.docs.ObservationDocumentation;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.*;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleProperties;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimplePropertiesConfigAdapter;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationProperties;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationRegistryCustomizer;
import org.springframework.boot.actuate.autoconfigure.tracing.MicrometerTracingAutoConfiguration;
import org.springframework.boot.actuate.metrics.MetricsEndpoint;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Random;

/**
 * 可观测性 Micrometer 概念启动器
 *
 * <pre>
 *
 * 系统的可观测性由三部分组成：
 *  1、指标（Metrics）
 *  2、分布式追踪（Traces）
 *  3、日志 （Logging）
 *
 * Spring Boot Actuator 产品级特性之一就是实现了系统的可观测性。
 * 它借助 Micrometer 来收集和处理指标、追踪数据，实现系统可观测性的前两个特性。
 * Micrometer 支持对收集的指标、追踪数据格式转换适配后发送到一个或多个监控系统中，如 Prometheus、Zipkin、Datadog 等。
 * Micrometer 采取类似 Slf4J 的门面模式，将指标、追踪数据标准化为一个统一的格式，使指标、追踪数据在系统中得以一次编码，
 * 适配各种不同的监控系统提供商。
 *
 * === 依赖 ===
 * Spring Boot Actuator 依赖 Micrometer 两个库：
 * 1. micrometer-core
 *  核心库，定义指标、分布式追踪等标准格式接口及指标注册器 {@link MeterRegistry}，
 *  同时内置一些标准指标的实现，例如：JVM 指标、HTTP 指标、数据库指标等。
 *
 * 2. micrometer-observation
 *  观察者库，定义对系统观测的编程接口，例如：{@link ObservationRegistry}、{@link Observation} 等。
 *
 * === 核心接口 ===
 * Spring Boot Actuator 通过自动配置机制完成对 Micrometer 的开箱即用，
 * 下面介绍自动配置中对 Micrometer 进行定制的配置及注意 Bean：
 *
 * 1. {@link MetricsAutoConfiguration}
 *  1.1 引入 {@link MetricsProperties} 指标属性配置
 *      以 management.metrics 为前缀的属性配置
 *  1.2 {@code MeterRegistryPostProcessor}
 *      - 负责将 {@link MeterRegistryCustomizer} 定制化应用到 {@link MeterRegistry} 上
 *      - 负责将 {@link MeterFilter} 过滤器注册到 {@link MeterRegistry} 上
 *      - 负责将容器中所有激活的预定义的 Metrics 注册到 {@link MeterRegistry}
 *
 * 2. {@link SimpleMetricsExportAutoConfiguration}
 *  2.1 引入 {@link SimpleProperties} 简单指标属性配置
 *      以 management.simple.metrics.export 为前缀的属性配置，其中属性：
 *      enabled - 是否启用将指标数据导出到 SimpleMeterRegistry
 *      step - 指标导出周期，默认 1 分钟
 *      mode - 计数模式，默认累加
 *  2.2 配置 {@link MeterRegistry} 抽象的简单实现 {@link SimpleMeterRegistry}
 *  2.3 配置 {@link MeterRegistryConfig} 接口的实现类 {@link SimplePropertiesConfigAdapter}
 *      用来获取 MeterRegistryConfig 中的属性配置的值
 *
 * 3. {@link CompositeMeterRegistryAutoConfiguration}
 *  将容器中已注册的 {@link MeterRegistry} 组合成一个 {@link CompositeMeterRegistry}
 *  并且标记此组合的 MeterRegistry 为默认的 MeterRegistry，达到对不同指标注册器的统一管理
 *
 * 4. {@link MicrometerTracingAutoConfiguration}
 *  配置 Micrometer 追踪 API，Spring Boot Actuator 默认没有引入该功能
 *
 * 5. {@link MetricsEndpointAutoConfiguration}
 *  5.1 引入 {@link MetricsEndpoint} 指标端点，用来将 {@link MeterRegistry} 收集的
 *      所有指标的名称暴露现实到 /actuator/metrics 端点中
 *
 * 6. {@link ObservationAutoConfiguration}
 *  6.1 引入 {@link ObservationProperties} 观察者属性配置
 *      以 management.observations 为前缀的属性配置
 *  6.2 {@code ObservationRegistryPostProcessor}
 *      - 实例化 {@code ObservationRegistryConfigurer}
 *      - 将 {@link ObservationPredicate} 注册到 {@link ObservationRegistry.ObservationConfig}
 *      - 将 {@link GlobalObservationConvention} 注册到 {@link ObservationRegistry.ObservationConfig}
 *      - 将 {@link ObservationHandler} 注册到 {@link ObservationRegistry.ObservationConfig}
 *      - 将 {@link ObservationFilter} 注册到 {@link ObservationRegistry.ObservationConfig}
 *      - 将 {@link ObservationRegistryCustomizer} 定制化应用到 {@link ObservationRegistry}
 *  6.3 {@link ObservationRegistry}
 *      向容器注册一个 {@link ObservationRegistry} Bean
 *  6.4 {@code ObservationHandlerGrouping}
 *      向容器注册具备对观测处理器按照指标、追踪类型进行分组的工具类
 *  6.5 {@code ObservationAutoConfiguration.MeterObservationHandlerConfiguration}
 *      根据类路径是否存在追踪 API，决定初始化默认的观察处理器 {@link MeterObservationHandler} 实现类
 *      不支持追踪时，初始化 {@link DefaultMeterObservationHandler} 实现类
 *      支持追踪时，初始化 {@code TracingAwareMeterObservationHandler} 实现类
 *      追踪 API 包含在 micrometer-tracing 库中，默认没有被引入
 *
 * === Micrometer 运作机制 ===
 * Micrometer 通过 {@link Observation} 对程序执行的各个阶段设置标准定义的观测点动作（例如：start、event、error），
 * 在观测点动作执行时会通知匹配支持的 {@link ObservationHandler} 观测处理器生成与动作相关的标准指标、追踪数据，
 * 同时将这些指标、追踪数据注册到该观测处理器绑定的指标、追踪注册器中，之后借由 {@link MeterRegistry} 向不同的监控系统发送指标、追踪数据。
 * 至此，Micrometer 完成了对系统程序的单次可观测编写，适配多种不同监控系统提供商的指标、追踪数据格式。
 *
 * 例如：
 *  当 {@link Observation} 执行 start 动作时，会通知指标观测处理器 {@link MeterObservationHandler} 生成
 *  {@link Meter} 指标子类实现，并注册到 {@link MeterRegistry} 中，达到对程序可观测性。
 *  同理，如果是追踪观测处理器 {@code TracingAwareMeterObservationHandler}，还会由 {@code Tracer}
 *  生成 {@code Span} 追踪数据。
 *
 * 针对不同的程序模块的观测，可以自定义实现 {@link ObservationDocumentation} 接口，遵照其规范定义该程序模块
 * 的指标名称的枚举、观测转换器等。
 *
 * === 项目配置 ===
 * 1. 利用 management.endpoints.web.exposure.include=metrics 开启 Web 端点，使指标暴露
 * 2. 定义一个 MeterRegistry 实现类 {@link LoggingMeterRegistry} 并将其注册到容器中
 *    实现定时将默认指标信息输出打印到日志
 *
 *
 * === 项目运行 ===
 * 1. 访问如下 URL 获得系统默认的指标名称：
 *  curl -X GET http://localhost:8080/actuator/metrics
 *
 * 2. 对 {@link DemoTarget} 观测的指标数据，可以访问如下 URL 查看：
 *
 *  查看 methodA 指标调用次数：
 *  curl -X GET http://localhost:8080/actuator/metrics/demo.methodA.count
 *
 *  查看 methodA、methodB 指标调用耗时：
 *  curl -X GET http://localhost:8080/actuator/metrics/demo.methodA.time
 *  curl -X GET http://localhost:8080/actuator/metrics/demo.methodB.time
 *
 * 3. 观察控制台输出，每隔 30 秒输出一次指标日志
 *
 * </pre>
 *
 * @author Reion
 * @date 2023-11-22
 **/
@SpringBootApplication
public class MicrometerConceptBootstrap {
    public static void main(String[] args) throws InterruptedException {
        ConfigurableApplicationContext ctx = SpringApplication.run(MicrometerConceptBootstrap.class, args);

        DemoTarget observationTarget = ctx.getBean(DemoTarget.class);
        Random random = new Random();
        while (true) {
            Thread.sleep(random.nextInt(2000));
            observationTarget.methodA();
        }
    }
}
