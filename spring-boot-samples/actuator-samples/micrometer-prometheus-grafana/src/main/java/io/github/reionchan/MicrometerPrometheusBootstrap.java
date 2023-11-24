package io.github.reionchan;

import io.github.reionchan.demo.DemoTarget;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.CollectorRegistry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus.PrometheusMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus.PrometheusProperties;
import org.springframework.boot.actuate.metrics.export.prometheus.PrometheusScrapeEndpoint;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Random;
import java.util.function.Consumer;

/**
 * Micrometer Prometheus Grafana 整合启动器
 *
 * <pre>
 * === Prometheus 自动装配 ===
 * 1. 引入依赖 micrometer-registry-prometheus
 * 2. Spring Cloud Actuator 自动装配类 {@link PrometheusMetricsExportAutoConfiguration}
 *  2.1 启用 {@link PrometheusProperties} 配置属性
 *      以 management.prometheus.metrics.export 为前缀的属性配置
 *  2.2 注册 {@link PrometheusConfig} 实例 Bean {@linkplain PrometheusPropertiesConfigAdapter}
 *  2.3 注册 {@link MeterRegistry} 实例 Bean {@link PrometheusMeterRegistry}
 *      1. 关联下方的 {@code CollectorRegistry} 实例 Bean
 *      2. 其创建 Micrometer 的规范的 {@link Meter} 的 Prometheus 实现指标时，
 *          将执行 {@link PrometheusMeterRegistry#applyToCollector(Meter.Id, Consumer) applyToCollector()} 方法
 *          将 Meter 注册到 Prometheus 的 CollectorRegistry 中，最后生成 Web 端点 /actuator/prometheus 的指标内容
 *  2.4 注册 {@link CollectorRegistry} 实例 Bean
 *      把 Prometheus 指标推送到 Prometheus 端点或 {@link PrometheusProperties.Pushgateway}
 *  2.5 注册 {@link PrometheusScrapeEndpoint} 的 Web 端点，默认路径为 /actuator/prometheus
 *
 * === 项目环境配置 ===
 * 1. 本机启动本项目
 *  1.1 编辑 application.yaml 暴露 health,metrics,prometheus 端点
 *  1.2 默认情况下，Prometheus 采取拉取方式获得 /actuator/prometheus 指标数据
 *      参考配置文件 management.prometheus.metrics.export.pushgateway 说明，
 *      可借由 Push Gateway 服务器推送指标到 Prometheus 服务器
 * 2. Docker 启动 Prometheus 和 Grafana 容器
 *  2.1 编辑 docker-compose.yaml 启动 Prometheus、Grafana 两个服务
 *  2.3 编辑 ./docker/prometheus/prometheus.yml 配置 Prometheus 服务器
 *
 * === 项目运行 ===
 * 1. 启动本项目
 * 2. 启动 Docker 容器中的 Prometheus 和 Grafana 服务
 *  2.1 当前工作目录：
 *      ./actuator-samples/micrometer-prometheus-grafana
 *  2.2 在工作目录执行如下命令：
 *      启动：
 *          docker-compose -f ./docker-compose.yaml up -d
 *      查看：
 *          docker-compose ps
 *      停止：
 *          docker-compose down
 *  2.3 访问 Prometheus 服务器：
 *      http://localhost:9090
 *  2.4 访问 Grafana 服务器：
 *      http://localhost:3000
 *      - 默认用户名密码：admin admin
 *      - 已经在下面的配置文件中设置了 Prometheus 数据源
 *          ./docker/grafana/provisioning/datasources/datasource.yml
 *
 *      通过设置查询表达式，将 demo.methodA、demo.methodB 调用情况展示在仪表盘上。
 * </pre>
 *
 * @author Reion
 * @date 2023-11-22
 **/
@SpringBootApplication
public class MicrometerPrometheusBootstrap {
    public static void main(String[] args) throws InterruptedException {
        ConfigurableApplicationContext ctx = SpringApplication.run(MicrometerPrometheusBootstrap.class, args);

        DemoTarget observationTarget = ctx.getBean(DemoTarget.class);
        Random random = new Random();
        while (true) {
            Thread.sleep(random.nextInt(2000));
            observationTarget.methodA();
        }
    }
}
