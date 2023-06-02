package io.github.reionchan;

import io.github.reionchan.config.RegistryRouteLocatorByBuilderConfig;
import io.github.reionchan.config.SecurityConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;

/**
 * Spring Cloud Gateway 网关请求控频示例
 *
 * 它是基于 Spring WebFlux 框架，采用 Reactive 非阻塞式编程实现的 API 网关。
 * 由于其是 Spring Cloud 生态体系成员，依托 Spring Cloud 的云原生特性，
 * 该网关对分布式集群的服务发现、负载均衡、服务熔断都有较好的支持。
 *
 * <pre>
 *  1. POM 中引入 spring-cloud-starter-gateway 依赖
 *
 *  2. 定义网关启动器 {@link GatewayRedisRateLimitBootstrap}
 *
 *  3. 使用 {@link RouteLocatorBuilder} 自定义路由规则
 *      3.1 自定义请求头 host 匹配、路径匹配、基于认证用户名进行自定义请求频率的 Buildable<Route>
 *          3.1.1 引入 spring-boot-starter-data-redis-reactive 依赖
 *          3.1.2 引入 spring-boot-starter-security 依赖
 *          3.1.3 在 application.yaml 中配置 Redis 连接参数
 *          3.1.4 编写 Spring Security 配置类 {@link SecurityConfig}
 *          3.1.5 在 {@link RegistryRouteLocatorByBuilderConfig} 中定义 {@link RedisRateLimiter} 类型的 Bean
 *          3.1.6 在 {@link RegistryRouteLocatorByBuilderConfig} 中编写控频路由
 *
 *     原理参考：
 *      {@link RegistryRouteLocatorByBuilderConfig#customRouteLocator}
 *
 * </pre>
 *
 * @author Reion
 * @date 2023-05-25
 **/
@SpringBootApplication
public class GatewayRedisRateLimitBootstrap {

    /**
     * 定义外网调试路由 URI, 它是一个简单的 HTTP 请求响应服务
     */
    public static final String ROUTE_URI = "http://httpbin.org";

    public static void main(String[] args) {
        SpringApplication.run(GatewayRedisRateLimitBootstrap.class, args);
    }
}
