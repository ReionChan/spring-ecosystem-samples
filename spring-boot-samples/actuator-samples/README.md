# Spring Boot Observability



## Concept

&emsp;&emsp;Introducing principal of **Micrometer** and how to custom some component to observe your code.

### [micrometer-concept](https://github.com/ReionChan/spring-ecosystem-samples/tree/main/spring-boot-samples/actuator-samples/micrometer-concept)

&emsp;&emsp;Custom `ObservationConvention`、`ObservationDocumentation`、`Observation.Context` to observe demo code `DemoTarget`.

&emsp;&emsp;Once you write observing code, then you get the main idea behind Observtion API :

> Instrument code once, and get multiple benefits out of it.

## Metrics

### [micrometer-metrics-prometheus]([micrometer-metrics-prometheus-grafana](https://github.com/ReionChan/spring-ecosystem-samples/tree/main/spring-boot-samples/actuator-samples/micrometer-metrics-prometheus))

* SpringBoot + Prometheus + Grafana

  ![](https://raw.githubusercontent.com/ReionChan/spring-ecosystem-samples/main/spring-boot-samples/actuator-samples/micrometer-metrics-prometheus/image/demo_methodA_dashboard_app2prometheus.png)

### [micrometer-metrics-opentelemetry](https://github.com/ReionChan/spring-ecosystem-samples/tree/main/spring-boot-samples/actuator-samples/micrometer-metrics-opentelemetry)

* SpringBoot + OpenTelemetry + Prometheus + Grafana

![](https://raw.githubusercontent.com/ReionChan/spring-ecosystem-samples/main/spring-boot-samples/actuator-samples/micrometer-metrics-opentelemetry/image/demo_methodA_dashboard_app2otel.png)

## Traces

### [micrometer-traces-opentelemetry](https://github.com/ReionChan/spring-ecosystem-samples/tree/main/spring-boot-samples/actuator-samples/micrometer-traces-opentelemetry)

* SpringBoot + OpenTelemetry + (Jaeger | Zipkin | Tempo) + Grafana

![](https://raw.githubusercontent.com/ReionChan/spring-ecosystem-samples/main/spring-boot-samples/actuator-samples/micrometer-traces-opentelemetry/image/demo_methodA_dashboard_app2otel_traces.png)

![](https://raw.githubusercontent.com/ReionChan/spring-ecosystem-samples/main/spring-boot-samples/actuator-samples/micrometer-traces-opentelemetry/image/otlp2jaeger_traces.png)

![](https://raw.githubusercontent.com/ReionChan/spring-ecosystem-samples/main/spring-boot-samples/actuator-samples/micrometer-traces-opentelemetry/image/otlp2zipkin_traces.png)

![](https://raw.githubusercontent.com/ReionChan/spring-ecosystem-samples/main/spring-boot-samples/actuator-samples/micrometer-traces-opentelemetry/image/dashboard2tempo_query.png)

## Logging

### [micrometer-logging-opentelemetry](https://github.com/ReionChan/spring-ecosystem-samples/tree/main/spring-boot-samples/actuator-samples/micrometer-logging-opentelemetry)

* SpringBoot + OpenTelemetry + Tempo + Loki + Grafana

![](https://raw.githubusercontent.com/ReionChan/spring-ecosystem-samples/main/spring-boot-samples/actuator-samples/micrometer-logging-opentelemetry/image/demo_methodA_dashboard_app2otel_logging.png)

![](https://raw.githubusercontent.com/ReionChan/spring-ecosystem-samples/main/spring-boot-samples/actuator-samples/micrometer-logging-opentelemetry/image/tempo_traceId2Logs_in_loki.png)
