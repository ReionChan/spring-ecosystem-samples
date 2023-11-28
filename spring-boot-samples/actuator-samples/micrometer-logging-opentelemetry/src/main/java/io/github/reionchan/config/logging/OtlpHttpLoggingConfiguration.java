package io.github.reionchan.config.logging;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.LogRecordProcessor;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.SdkLoggerProviderBuilder;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.tracing.ConditionalOnEnabledTracing;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * 基于 OTLP Http 协议的日志配置（目前 Spring Boot 未自动装配）
 *
 * @author Reion
 * @date 2023-11-28
 **/
@Configuration
@EnableConfigurationProperties(OtlpProperties.class)
public class OtlpHttpLoggingConfiguration {

    @Bean
    BatchLogRecordProcessor batchLogRecordProcessor(OtlpProperties properties) {
        return BatchLogRecordProcessor.builder(
                OtlpHttpLogRecordExporter.builder()
                   .setEndpoint(properties.getEndpoint())
                   .setTimeout(properties.getTimeout()).build()
        ).build();
    }

    @Bean
    SdkLoggerProvider sdkLoggerProvider(Environment environment, ObjectProvider<LogRecordProcessor> loggerRecordProcessors) {
        String applicationName = environment.getProperty("spring.application.name", "application");
        Resource springResource = Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, applicationName));
        SdkLoggerProviderBuilder builder = SdkLoggerProvider.builder()
                .setResource(Resource.getDefault().merge(springResource));
        loggerRecordProcessors.orderedStream().forEach(builder::addLogRecordProcessor);
        return builder.build();
    }

    @Bean
    OpenTelemetry openTelemetry(SdkTracerProvider sdkTracerProvider, SdkLoggerProvider sdkLoggerProvider, ContextPropagators contextPropagators) {
        OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(sdkTracerProvider)
                .setLoggerProvider(sdkLoggerProvider)
                .setPropagators(contextPropagators)
                .build();
        OpenTelemetryAppender.install(openTelemetry);
        return openTelemetry;
    }
}
