package io.github.reionchan;

import io.github.reionchan.demo.DemoTarget;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.push.PushMeterRegistry;
import io.micrometer.core.ipc.http.HttpSender;
import io.micrometer.core.ipc.http.HttpUrlConnectionSender;
import io.micrometer.registry.otlp.OtlpConfig;
import io.micrometer.registry.otlp.OtlpMeterRegistry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.export.otlp.OtlpMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.otlp.OtlpProperties;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Random;

/**
 * Micrometer OpenTelemetry 整合启动器
 *
 * <pre>
 * === OpenTelemetry 自动装配 ===
 * 1. 引入依赖 micrometer-registry-otlp
 * 2. Spring Cloud Actuator 自动装配类 {@link OtlpMetricsExportAutoConfiguration}
 *  2.1 启用 {@link OtlpProperties} 配置属性
 *      以 management.otlp.metrics.export 为前缀的属性配置
 *      其中默认推送地址为：http://localhost:4318/v1/metrics
 *  2.2 注册 {@link OtlpConfig} 实例 Bean {@linkplain OtlpPropertiesConfigAdapter}
 *  2.3 注册 {@link MeterRegistry} 实例 Bean {@link OtlpMeterRegistry}
 *      1. 它继承 {@link PushMeterRegistry} 抽象类，属于主动推送型 MeterRegistry
 *      2. 它关联 {@link HttpSender} 接口的实现类 {@linkplain HttpUrlConnectionSender} 完成 Http 协议的推送
 *         它将 Micrometer 的规范的 {@link Meter} 转换为 OTLP 协议的格式 {@linkplain Metric} 后，
 *         执行 {@link OtlpMeterRegistry#publish()} 方法推送数据
 *
 * === 项目环境配置 ===
 * 1. Docker 启动 Prometheus 和 Grafana 容器
 *  1.1 编辑 docker-compose.yaml 启动 OpenTelemetry Collector、Prometheus 及 Grafana 服务
 *  1.2 编辑 ./docker/opentelemetry-collector/otel-collector-config.yaml 配置 OpenTelemetry Collector 服务器
 *      设置 Prometheus 导出器、服务的 pipelines 指标中添加 prometheus
 *  1.3 编辑 ./docker/prometheus/prometheus.yml 配置 Prometheus 服务器
 *      设置 Prometheus 监控指标数据抓取的目标服务器地址为 otel-collector:8889
 * 2. 本机启动本项目
 *  2.1 编辑 application.yaml，启用 OTLP 导出设置
 *      以 management.otlp.metrics.export 开头的属性配置
 *      暴露 Web 端点 health,metrics
 *
 * === 项目运行 ===
 * 1. 启动 Docker 容器中的 OpenTelemetry Collector、Prometheus 和 Grafana 服务
 *  1.1 当前工作目录：
 *      ./actuator-samples/micrometer-opentelemetry
 *  1.2 在工作目录执行如下命令：
 *      启动：
 *          docker-compose -f ./docker-compose.yaml up -d
 *      查看：
 *          docker-compose ps
 *      停止：
 *          docker-compose down
 *  1.3 访问 Prometheus 服务器：
 *      http://localhost:9090
 *  1.4 访问 Grafana 服务器：
 *      http://localhost:3000
 *      - 默认用户名密码：admin admin
 *      - 已经在下面的配置文件中设置了 Prometheus 数据源
 *          ./docker/grafana/provisioning/datasources/datasource.yml
 * 2. 启动本项目
 *  启动时，将会尝试连接 OpenTelemetry Collector 服务器，故先执行上方 Docker Compose，
 *  启动 OpenTelemetry Collector 服务。
 *  通过设置查询表达式，将 demo.methodA、demo.methodB 调用情况展示在仪表盘上。
 * </pre>
 *
 * @author Reion
 * @date 2023-11-22
 **/
@SpringBootApplication
public class MicrometerOpenTelemetryBootstrap {
    public static void main(String[] args) throws InterruptedException {
        ConfigurableApplicationContext ctx = SpringApplication.run(MicrometerOpenTelemetryBootstrap.class, args);

        DemoTarget observationTarget = ctx.getBean(DemoTarget.class);
        Random random = new Random();
        while (true) {
            Thread.sleep(random.nextInt(2000));
            observationTarget.methodA();
        }
    }
}
