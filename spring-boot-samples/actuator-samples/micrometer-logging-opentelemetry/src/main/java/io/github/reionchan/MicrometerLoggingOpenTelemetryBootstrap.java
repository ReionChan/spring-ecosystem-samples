package io.github.reionchan;

import io.github.reionchan.config.logging.OtlpHttpLoggingConfiguration;
import io.github.reionchan.config.logging.OtlpProperties;
import io.github.reionchan.demo.DemoTarget;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Random;

/**
 * Micrometer OpenTelemetry OTLP to Tempo and Loki 整合观察追踪日志数据启动器
 *
 * <pre>
 *
 * 本项目采用 OpenTelemetry 规范的 OTLP 协议，将 Micrometer 规范的指标、追踪、日志数据
 * 发送到 OpenTelemetry Collector 服务器，然后利用它支持多种监控后端的导出器的特性
 * 将数据推送到 Tempo、Loki 监控后端。
 *
 * === OpenTelemetry 自动装配 ===
 *
 * 1. 引入依赖 micrometer-registry-otlp
 *   OTLP 协议推送指标数据
 * 2. 引入依赖 micrometer-tracing
 *   Micrometer 对追踪的抽象 API
 * 3. 引入依赖 micrometer-tracing-bridge-otel
 *   将 Micrometer-tracing API 桥接到 OpenTelemetry API 实现
 *   类似于 Slf4J 与 Logback 的桥接
 * 4. 引入依赖 opentelemetry-exporter-otlp
 *   OTLP 协议的导出器
 * 5. 引入依赖 opentelemetry-logback-appender-1.0
 *   OTLP 日志记录器，将日志记录转发给 OTLP 导出器
 *
 * === Spring Boot Actuator 自动装配 ===
 *
 * 1. 自定义日志配置类 {@link OtlpHttpLoggingConfiguration}
 *  1.1 启用 {@link OtlpProperties} 配置属性
 *      以 management.otlp.logging 为前缀的属性配置
 *      其中默认推送地址为：http://localhost:4318/v1/logs
 *
 *  1.2 注册 {@link BatchLogRecordProcessor}
 *      使用基于 HTTP 的 OTLP 协议导出追踪数据 {@link OtlpHttpLogRecordExporter}
 *
 *  1.3 注册 {@link SdkLoggerProvider}
 *      用来提供日志发送服务的提供者，其关联日志记录处理器
 *
 *  1.4. 自定义覆盖注册 {@link OpenTelemetry} 实现类 {@link OpenTelemetrySdk}
 *      用来插装日志收集发送功能的提供者，并将 {@link OpenTelemetryAppender}
 *      关联 {@link OpenTelemetry}，完成日志 API 调用的侦测及日志记录转发
 *
 * === 项目环境配置 ===
 * 1. Docker Compose 配置
 *  1.1 编辑 docker-compose.yaml 添加 Loki、Tempo 服务
 *  1.2 编辑 ./docker/opentelemetry-collector/otel-collector-config.yaml 配置 Loki, Tempo 导出器
 *      服务的 pipelines 追踪项目中添加 zipkin、otlp (Jaeger、Tempo 目前不在导出器支持名称中，变相修改为其支持的 otlp 名称)
 *  1.3 编辑 ./docker/prometheus/prometheus.yml 配置 Prometheus 服务器
 *      设置 Prometheus 监控指标数据抓取的目标服务器地址为 otel-collector:8889
 *  1.4 编辑 ./docker/grafana/provisioning/datasources/datasource.yml 配置 Grafana 数据源
 *      新增 Tempo 数据源配置，方便观察追踪数据查询及图表
 * 2. 本机项目变更
 *  2.1 编辑 application.yaml，启用 OTLP 导出设置
 *      - 以 management.otlp 开头的属性配置添加 logging，配置日志导出地址
 *      - 暴露 Web 端点 health,metrics
 *      - 配置 logging.pattern.level 日志等级格式中追加显示 MDC 中的 traceId/spanId 信息
 *
 * === 项目运行 ===
 * 1. 启动 Docker 容器中的 OpenTelemetry Collector、Loki、Tempo、Prometheus 和 Grafana 服务
 *  1.1 当前工作目录：
 *      ./actuator-samples/micrometer-logging-opentelemetry
 *  1.2 在工作目录执行如下命令：
 *      启动：
 *          docker-compose -f ./docker-compose.yaml up -d
 *      查看：
 *          docker-compose ps
 *      停止：
 *          docker-compose down
 *  1.3 访问 Grafana 服务器，配置 Tempo 追踪链接 Loki 日志设置
 *      http://localhost:3000
 *      用户名、密码：admin/admin
 *  1.4 Grafana 服务器查看 Tempo 追踪数据，并点击 TraceId 链接查看追踪日志
 *
 *
 * 2. 启动本项目
 *  启动时，将会尝试连接 OpenTelemetry Collector 服务器，故先执行上方 Docker Compose，
 *  启动 OpenTelemetry Collector 服务。
 *  可以观察到每次调用会形成包含 2 个 Span 的调用链，点击 Logs for this span，
 *  即可打开 Loki 视图查看相对应的日志记录。
 *
 * </pre>
 *
 * @author Reion
 * @date 2023-11-28
 **/
@SpringBootApplication
public class MicrometerLoggingOpenTelemetryBootstrap {
    public static void main(String[] args) throws InterruptedException {
        ConfigurableApplicationContext ctx = SpringApplication.run(MicrometerLoggingOpenTelemetryBootstrap.class, args);

        DemoTarget observationTarget = ctx.getBean(DemoTarget.class);
        Random random = new Random();
        while (true) {
            Thread.sleep(1000 + random.nextInt(2000));
            observationTarget.methodA();
        }
    }
}
