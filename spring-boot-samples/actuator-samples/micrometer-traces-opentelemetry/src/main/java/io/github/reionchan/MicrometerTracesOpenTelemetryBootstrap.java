package io.github.reionchan;

import io.github.reionchan.demo.DemoTarget;
import io.github.reionchan.demo.observation.handler.DemoTracingObservationHandler;
import io.micrometer.core.instrument.Meter;
import io.micrometer.observation.transport.ReceiverContext;
import io.micrometer.observation.transport.SenderContext;
import io.micrometer.registry.otlp.OtlpMeterRegistry;
import io.micrometer.tracing.Baggage;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.handler.DefaultTracingObservationHandler;
import io.micrometer.tracing.handler.PropagatingReceiverTracingObservationHandler;
import io.micrometer.tracing.handler.PropagatingSenderTracingObservationHandler;
import io.micrometer.tracing.otel.bridge.OtelCurrentTraceContext;
import io.micrometer.tracing.otel.bridge.OtelTracer;
import io.micrometer.tracing.otel.bridge.Slf4JEventListener;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.tracing.MicrometerTracingAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.tracing.OpenTelemetryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.tracing.otlp.OtlpAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.tracing.otlp.OtlpProperties;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Random;

/**
 * Micrometer OpenTelemetry OTLP to Zipkin Jaeger Tempo 整合观察追踪数据启动器
 *
 * 本项目采用 OpenTelemetry 规范的 OTLP 协议，将 Micrometer 规范的指标、追踪数据
 * 发送到 OpenTelemetry Collector 服务器，然后利用它支持多种监控后端的导出器的特性
 * 将数据推送到 Zipkin、Jaeger、Tempo 监控后端。
 *
 * 注意：
 *     SpringBoot 3.1.0+ 之后才支持自动装配 OTLP 协议的追踪数据导出 {@link OtlpAutoConfiguration}
 *
 * <pre>
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
 *
 * === Spring Boot Actuator 自动装配 ===
 *
 * 1. 自动装配类 {@link OtlpAutoConfiguration}
 *  1.1 启用 {@link OtlpProperties} 配置属性
 *      以 management.otlp.tracing 为前缀的属性配置
 *      其中默认推送地址为：http://localhost:4318/v1/traces
 *      注意：
 *          区别同名不同包的 {@link org.springframework.boot.actuate.autoconfigure.metrics.export.otlp.OtlpProperties}
 *  1.2 注册 {@link OtlpHttpSpanExporter}
 *      使用基于 HTTP 的 OTLP 协议导出追踪数据（默认装载）
 *      另外还支持 gRPC 的 OTLP 导出器 {@link OtlpGrpcSpanExporter}
 *
 * 2. 自动装配类 {@link OpenTelemetryAutoConfiguration}
 *  2.1 注册 {@link OpenTelemetry} 实现类 {@link OpenTelemetrySdk}
 *      【重要】OpenTelemetry 对指标、追踪、日志三种类别观测数据、传播器的提供商策略接口
 *
 *              | Micrometer-API |      Bridge Package               |    OpenTelemetry-API
 *     ===============================================================================================================
 *     Metrics  | {@link Meter}  |  Micrometer-registry-otlp         | {@linkplain io.opentelemetry.api.metrics.Meter}
 *              |                |  {@link OtlpMeterRegistry} 转化    | {@literal io.opentelemetry.proto.metrics.v1.Metric}
 *     ---------------------------------------------------------------------------------------------------------------
 *     Traces   | {@link Tracer} |  Micrometer-tracing-bridge-otel    | {@link io.opentelemetry.api.trace.Tracer}
 *              | {@link Span}   |  OtelTracer、OtelSpan 适配          | {@link io.opentelemetry.api.trace.Span}
 *     ---------------------------------------------------------------------------------------------------------------
 *     Logging  |                | opentelemetry-logback-appender-1.0 | {@link io.opentelemetry.api.logs.Logger}
 *              |                | OpenTelemetryAppender} 适配         | {@link io.opentelemetry.api.logs.LogRecordBuilder}
 *     ---------------------------------------------------------------------------------------------------------------
 *     其中，在系统观测时，生成的 Span 如何借由 OtelSpan 发送到 OpenTelemetry Collector 服务器原理：
 *     1. {@link Span.Builder} 生成的 {@link Span} 实例，交由桥接适配的 {@link OtelTracer} 生成 OtelSpan
 *        而 OtelSpan 包装适配 {@link io.opentelemetry.api.trace.Span} 的实现类 {@linkplain io.opentelemetry.sdk.trace.SdkSpan}
 *        它实现 {@linkplain ReadWriteSpan}，它关联 {@link SpanProcessor} 处理器 (下面 2.5 装载的批处理 Span 处理器)
 *        代码参考 {@link io.micrometer.tracing.otel.bridge.OtelSpanBuilder#start()}：
 *        {@code
 *          public Span start() {
 *              // 省略代码，此处 tracer 为 SdkTracer，SpanBuilder 为 SdkSpanBuilder
 *              SpanBuilder spanBuilder = this.tracer.spanBuilder(StringUtils.isNotEmpty(this.name) ? this.name : "");
 *              io.opentelemetry.api.trace.Span span = spanBuilder.startSpan();
 *              return OtelSpan.fromOtel(span);
 *          }
 *        }
 *     2. 之后所有对 Micrometer {@link Span} API 操作都借由 OtelSpan 委托给 {@linkplain io.opentelemetry.sdk.trace.SdkSpan} 执行
 *        在 onEnd 时，它会通知其关联的 {@link SpanProcessor} 处理当前 Span 的发送，例如：
 *        {@link BatchSpanProcessor#onEnd(ReadableSpan)} 将把 Span 交给发送工作线程 work 处理：
 *        {@code
 *          @Override
 *          public void onEnd(ReadableSpan span) {
 *              if (span == null || !span.getSpanContext().isSampled()) {
 *                  return;
 *              }
 *              worker.addSpan(span);
 *          }
 *        }
 *        它默认等到达到规定批量发送数量或超过 30 秒就发送当前收集的所有 Span。
 *
 *  2.2 注册 {@link SdkTracerProvider}
 *      用来获得 {@code SdkTracer} 实例的提供者
 *  2.3 注册 {@link ContextPropagators}
 *      用来获获得在边界传递时对追踪数据的注入与提取的传播器，默认基于文本格式的传播器
 *  2.4 注册 {@link Sampler}
 *      用来对 Span 采样的决策器，例如：采样率设置
 *  2.5 注册 {@link SpanProcessor}
 *      默认装载 {@link BatchSpanProcessor} 批处理形式的 Span 处理器
 *      它依赖上面注入的 {@link OtlpHttpSpanExporter}、及一些拦截、过滤、报告等处理组件
 *  2.6 注册 {@link Slf4JEventListener}
 *      【重要】将追踪信息更新到 Slf4J 的 MDC，这些事件监听器由 {@link OtelTracer.EventPublisher} 收集通知
 *      而后者被纳入追踪上下文 CurrentTraceContext 实现类 {@link OtelCurrentTraceContext}
 *  2.7 注册 {@link TextMapPropagator}
 *      该传播器支持 management.tracing.baggage 前缀的属性配置
 *      它能将 {@link Baggage} 跨越边界传递，也由此命名为 Baggage 传播器
 *
 * 3. 自动装配类 {@link MicrometerTracingAutoConfiguration}
 *  该类注册三个排序的 TracingObservationHandler 实现类：
 *    1. {@link PropagatingReceiverTracingObservationHandler} 优先级最高
 *      当发生接收信息时触发的处理器，关联 {@link ReceiverContext} 上下文
 *      由传播器从接收上下文关联的载体中提取信息
 *    2. {@link PropagatingSenderTracingObservationHandler} 优先级适中
 *      当发生发送信息时触发的处理器，关联 {@link SenderContext} 上下文
 *      由传播器将上下文的信息注入到发送上下文关联的载体
 *    3. {@link DefaultTracingObservationHandler} 优先级最低
 *      默认在观测开始是创建跟踪，结束是关闭跟踪的兜底处理器
 *
 * === 项目环境配置 ===
 * 1. Docker Compose 配置
 *  1.1 编辑 docker-compose.yaml 添加 Zipkin、Jaeger、Tempo 服务
 *  1.2 编辑 ./docker/opentelemetry-collector/otel-collector-config.yaml 配置 Zipkin、Jaeger、Tempo 导出器
 *      服务的 pipelines 追踪项目中添加 zipkin、otlp (Jaeger、Tempo 目前不在导出器支持名称中，变相修改为其支持的 otlp 名称)
 *  1.3 编辑 ./docker/prometheus/prometheus.yml 配置 Prometheus 服务器
 *      设置 Prometheus 监控指标数据抓取的目标服务器地址为 otel-collector:8889
 *  1.4 编辑 ./docker/grafana/provisioning/datasources/datasource.yml 配置 Grafana 数据源
 *      新增 Tempo 数据源配置，方便观察追踪数据查询及图表
 * 2. 本机项目变更
 *  2.1 编辑 application.yaml，启用 OTLP 导出设置
 *      - 以 management.otlp.metrics.export 开头的属性配置添加 tracing，配置采样率及导出地址
 *      - 暴露 Web 端点 health,metrics
 *      - 配置 logging.pattern.level 日志等级格式中追加显示 MDC 中的 traceId/spanId 信息
 *  2.2 新增自定义追踪处理器 {@link DemoTracingObservationHandler} 注意其排序 @Order
 *      它支持在新开启 Scope 时，在原由的 Span 里启用嵌套子 Span，模拟一个跨边界的调用
 *
 * === 项目运行 ===
 * 1. 启动 Docker 容器中的 OpenTelemetry Collector、Prometheus 和 Grafana 服务
 *  1.1 当前工作目录：
 *      ./actuator-samples/micrometer-traces-opentelemetry
 *  1.2 在工作目录执行如下命令：
 *      启动：
 *          docker-compose -f ./docker-compose.yaml up -d
 *      查看：
 *          docker-compose ps
 *      停止：
 *          docker-compose down
 *  1.3 访问 Zipkin 服务器查看跟踪数据：
 *      http://localhost:9411
 *  1.4 访问 Jaeger 服务器查看跟踪数据：
 *      http://localhost:16686
 *
 *      当然，也可以在 Grafana 添加 Zipkin、Jaeger、Tempo 数据源查看
 *
 * 2. 启动本项目
 *  启动时，将会尝试连接 OpenTelemetry Collector 服务器，故先执行上方 Docker Compose，
 *  启动 OpenTelemetry Collector 服务。
 *  可以观察到每次调用会形成包含 2 个 Span 的调用链。
 * </pre>
 *
 * @author Reion
 * @date 2023-11-22
 **/
@SpringBootApplication
public class MicrometerTracesOpenTelemetryBootstrap {
    public static void main(String[] args) throws InterruptedException {
        ConfigurableApplicationContext ctx = SpringApplication.run(MicrometerTracesOpenTelemetryBootstrap.class, args);

        DemoTarget observationTarget = ctx.getBean(DemoTarget.class);
        Random random = new Random();
        while (true) {
            Thread.sleep(random.nextInt(1000));
            observationTarget.methodA();
        }
    }
}
