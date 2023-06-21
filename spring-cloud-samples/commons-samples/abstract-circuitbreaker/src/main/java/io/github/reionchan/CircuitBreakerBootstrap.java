package io.github.reionchan;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import java.util.Random;

/**
 * 熔断降级启动器
 *
 * <pre>
 * 熔断降级与服务自动注册相似，spring-cloud-commons 仅留有抽象接口定义，
 * 而具体的实现已放入独立的 spring-cloud-circuitbreaker 项目，该项目
 * 适配 Resilience4j、Spring Retry 两种技术的熔断降级实现，
 * 此外 Alibaba Sentinal 同样也适配支持 Spring Cloud 对熔断降级的抽象。
 *
 * 本示例侧重介绍 commons 模块对熔断降级抽象接口：
 *  {@link CircuitBreakerFactory}、{@link Customizer}、
 *  {@link org.springframework.cloud.client.circuitbreaker.CircuitBreaker}
 * 引入 spring-cloud-circuitbreaker-resilience4j 模块仅是为了辅助演示需求，后者的详细示例
 * 将放在 circuitbreaker-samples 模块。
 *
 * </pre>
 *
 * @author Reion
 * @date 2023-06-20
 **/
@Slf4j
@SpringBootApplication
public class CircuitBreakerBootstrap {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(CircuitBreakerBootstrap.class, args);

        /**
         * 获得容器中自动装载的 {@link CircuitBreakerFactory} 熔断器工厂 Bean
         */
        CircuitBreakerFactory circuitBreakerFactory = context.getBean(CircuitBreakerFactory.class);
        log.info("=== CircuitBreakerFactory 实现类：{}", circuitBreakerFactory.getClass().getSimpleName());
        Random random = new Random();
        String ret = null;
        for (int i=0; i<5; i++) {
            // 执行 id 为 slow 的被熔断器包含的随机睡眠操作（睡眠超过 1 秒发生熔断保护）
            ret = circuitBreakerFactory.create("slow").run(() -> {
                try {
                    Thread.sleep(random.nextInt(3) * 1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                return "Success!";
            }, (t) -> "Whoops! Fallback...");
            log.info("Try it and result: {}\n", ret);
        }
        Environment env = context.getEnvironment();
        log.info("{} {} {} {}",
                env.getProperty("mykeys.key1"),
                env.getProperty("mykeys.key2"),
                env.getProperty("mykeys.key3"),
                env.getProperty("mykeys.key4"));
    }

    /**
     * {@link Customizer} 接口对 {@link Resilience4JCircuitBreakerFactory} 进行定制
     */
    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> custom() {
        Customizer<Resilience4JCircuitBreakerFactory> customizer = Customizer.once(
                factory -> {
                    factory.getCircuitBreakerRegistry().circuitBreaker("slow").getEventPublisher().onError(event -> {
                        log.info("【onError】{} takes {} ms!", event.getCircuitBreakerName(), event.getElapsedDuration().toMillis());
                    }).onSuccess(event -> {
                        log.info("【onSuccess】{} takes {} ms!", event.getCircuitBreakerName(), event.getElapsedDuration().toMillis());
                    });
                },

                factory -> factory.create("slow")
        );
        return customizer;
    }
}
