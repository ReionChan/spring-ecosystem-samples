package io.github.reionchan;

import io.github.reionchan.config.RegistryRouteDefinitionByInMemoryRepositoryConfig;
import io.github.reionchan.config.RegistryRouteLocatorByBuilderConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.config.GatewayResilience4JCircuitBreakerAutoConfiguration;
import org.springframework.cloud.gateway.config.PropertiesRouteDefinitionLocator;
import org.springframework.cloud.gateway.filter.factory.FallbackHeadersGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.SpringCloudCircuitBreakerResilience4JFilterFactory;
import org.springframework.cloud.gateway.route.InMemoryRouteDefinitionRepository;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;

/**
 * Spring Cloud Gateway 自定义路由的几种方法示例
 *
 * 它是基于 Spring WebFlux 框架，采用 Reactive 非阻塞式编程实现的 API 网关。
 * 由于其是 Spring Cloud 生态体系成员，依托 Spring Cloud 的云原生特性，
 * 该网关对分布式集群的服务发现、负载均衡、服务熔断都有较好的支持。
 *
 * <pre>
 *  1. POM 中引入 spring-cloud-starter-gateway 依赖
 *
 *  2. 定义网关启动器 {@link GatewayBasicBootstrap}
 *
 *  3. 使用 {@link InMemoryRouteDefinitionRepository} 自定义路由规则
 *      3.1 自定义请求路径 Path 匹配的 RouteDefinition
 *
 *     原理参考：
 *      {@link RegistryRouteDefinitionByInMemoryRepositoryConfig#inMemoryRouteDefinition} 注释
 *
 *  4. 使用 {@link RouteLocatorBuilder} 自定义路由规则
 *      4.1 自定义请求路径 path 匹配的 Buildable<Route>
 *      4.2 自定义请求头 host 匹配的 Buildable<Route>
 *      4.3 自定义请求头 host 匹配、请求路径重写的 Buildable<Route>
 *
 *     原理参考：
 *      {@link RegistryRouteLocatorByBuilderConfig#customRouteLocator}
 *
 *  5. 使用 {@link PropertiesRouteDefinitionLocator} 自定义路由规则
 *      5.1 在 application.yaml 中设置 spring.cloud.gateway.routes 属性自定义 RouteDefinition
 *
 *     原理参考：
 *      {@link PropertiesRouteDefinitionLocator} 也继承 {@link RouteDefinitionLocator}，
 *      故原理同 {@link InMemoryRouteDefinitionRepository} 一致
 * </pre>
 *
 * @author Reion
 * @date 2023-05-25
 **/
@SpringBootApplication
public class GatewayBasicBootstrap {

    /**
     * 定义外网调试路由 URI, 它是一个简单的 HTTP 请求响应服务
     */
    public static final String ROUTE_URI = "http://httpbin.org";

    public static void main(String[] args) {
        SpringApplication.run(GatewayBasicBootstrap.class, args);
    }
}
