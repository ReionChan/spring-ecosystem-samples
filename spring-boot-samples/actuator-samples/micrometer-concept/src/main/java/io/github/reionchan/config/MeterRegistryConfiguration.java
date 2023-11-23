package io.github.reionchan.config;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.logging.LoggingMeterRegistry;
import io.micrometer.core.instrument.logging.LoggingRegistryConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 指标注册器配置类
 *
 * @author Reion
 * @date 2023-11-22
 **/
@Configuration
public class MeterRegistryConfiguration {
    /**
     * 向容器中注册基于日志的 MeterRegistry 实现类 LoggingMeterRegistry
     *
     * 该注册器设置每隔 10 秒输出一次指标日志
     */
    @Bean
    public MeterRegistry loggerMeterRegistry() {
        LoggingRegistryConfig config = new LoggingRegistryConfig() {
            /**
             * 自定义每隔 30 秒输出一次指标日志
             */
            @Override
            public Duration step() {
                return Duration.ofSeconds(30);
            }

            @Override
            public String get(String key) {
                return null;
            }
        };

        return new LoggingMeterRegistry(config, Clock.SYSTEM);
    }
}
