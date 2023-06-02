package io.github.reionchan;

import io.github.reionchan.config.RegistryRouteLocatorByBuilderConfig;
import io.github.reionchan.controller.FallbackController;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.config.GatewayResilience4JCircuitBreakerAutoConfiguration;
import org.springframework.cloud.gateway.filter.factory.FallbackHeadersGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.SpringCloudCircuitBreakerResilience4JFilterFactory;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;

/**
 * Spring Cloud Gateway 服务熔断示例
 *
 * 它是基于 Spring WebFlux 框架，采用 Reactive 非阻塞式编程实现的 API 网关。
 * 由于其是 Spring Cloud 生态体系成员，依托 Spring Cloud 的云原生特性，
 * 该网关对分布式集群的服务发现、负载均衡、服务熔断都有较好的支持。
 *
 * <pre>
 *  1. POM 中引入 spring-cloud-starter-gateway 依赖
 *
 *  2. 定义网关启动器 {@link GatewayCircuitBreakerBootstrap}
 *
 *  3. 使用 {@link RouteLocatorBuilder} 自定义路由规则
 *      3.1 自定义请求头 host 匹配、指定慢请求配置、具备熔断功能的 Buildable<Route>
 *          3.1.1 引入 spring-cloud-starter-circuitbreaker-reactor-resilience4j 依赖
 *          3.1.2 激活自动化配置 {@link GatewayResilience4JCircuitBreakerAutoConfiguration}
 *          3.1.3 注册 {@link SpringCloudCircuitBreakerResilience4JFilterFactory} 类型的 Bean
 *          3.1.4 注册 {@link FallbackHeadersGatewayFilterFactory} 类型的 Bean
 *      3.2 自定义请求头 host 匹配、指定慢请求配置、具备熔断功能、断路补偿功能的 Buildable<Route>
 *          3.2.1 编写熔断补偿控制器端点 {@link FallbackController}
 *
 *     原理参考：
 *      {@link RegistryRouteLocatorByBuilderConfig#customRouteLocator}
 * </pre>
 *
 * @author Reion
 * @date 2023-05-25
 **/
@SpringBootApplication
public class GatewayCircuitBreakerBootstrap {

    /**
     * 定义外网调试路由 URI, 它是一个简单的 HTTP 请求响应服务
     */
    public static final String ROUTE_URI = "http://httpbin.org";

    public static void main(String[] args) {
        SpringApplication.run(GatewayCircuitBreakerBootstrap.class, args);
    }
}
