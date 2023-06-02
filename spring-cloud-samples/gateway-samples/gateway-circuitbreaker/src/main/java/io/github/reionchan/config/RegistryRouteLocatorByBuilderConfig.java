package io.github.reionchan.config;

import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.cloud.gateway.config.GatewayAutoConfiguration;
import org.springframework.cloud.gateway.route.CachingRouteLocator;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.DispatcherHandler;

import java.time.Duration;
import java.util.List;

import static io.github.reionchan.GatewayCircuitBreakerBootstrap.ROUTE_URI;

/**
 * 通过 RouteLocatorBuilder 注册自定义 RouteLocator
 *
 * @author Reion
 * @date 2023-05-25
 **/
@Configuration
@CommonsLog
public class RegistryRouteLocatorByBuilderConfig {

    /**
     * 通过 Spring Cloud Gateway 自动装配的 {@link GatewayAutoConfiguration#routeLocatorBuilder} 构建器 Bean
     * 注册自定义 RouteLocator，向网关配置路由信息。
     *
     * <pre>
     * 原理：
     *      {@link RouteLocatorBuilder} Bean 构建出的 {@link RouteLocator}
     *      由 {@link GatewayAutoConfiguration#cachedCompositeRouteLocator(List)} 收集，
     *      整合成 {@link CachingRouteLocator}，
     *      最终被纳入 {@link GatewayAutoConfiguration#routePredicateHandlerMapping} 路由断言处理映射器的属性
     *      当客户端请求时，由 {@link DispatcherHandler} 交给此路由断言处理映射器进行断言匹配路由到所配置的 URI
     * </pre>
     */
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        log.info("--- 自定义具备断路器功能的路由 ---");
        return builder.routes()
                // 路径匹配路由
                .route("path_route", r -> r.path("/get")
                        .uri(ROUTE_URI))

                // 头部字段 host 匹配路由
                .route("host_route", r -> r.host("*.myhost.org")
                        .uri(ROUTE_URI))

                // 路径重写路由
                .route("rewrite_route", r -> r.host("*.rewrite.org")
                        .filters(f -> f.rewritePath("/foo/(?<segment>.*)",
                                "/${segment}"))
                        .uri(ROUTE_URI))

                // 断路器路由 （customizerCircuitBreakerFactory 指定接口响应超时时间 5 秒，超时后激发断路器断路，返回 504 Gateway timeout）
                // 尝试请求 http://localhost:8080/delay/1   http://localhost:8080/delay/6
                // 将路由到 http://httpbin.org/delay/1      http://httpbin.org/delay/6
                //        延时 1 秒的不会被断路器熔断           延时 3 秒的由于超过 2 秒超时时间，发生熔断，返回 504 状态码
                .route("circuitbreaker_route", r -> r.host("*.circuitbreaker.org")
                        .filters(f -> f.circuitBreaker(c -> c.setName("slowcmd")))
                        .uri(ROUTE_URI))

                // 与上方效果区别在于发生超时熔断时，将会触发 fallback 操作，将请求转发到指定的 fallbackUri
                .route("circuitbreaker_fallback_route", r -> r.host("*.circuitbreakerfallback.org")
                        .filters(f -> f.circuitBreaker(c -> c.setName("slowcmd").setFallbackUri("forward:/circuitbreakerfallback")))
                        .uri(ROUTE_URI))
                .build();
    }

    /**
     * 自定义断路器的超时时间，默认 1 秒钟，此处修改为 2 秒钟
     *
     * 设定配置的名称为：slowcmd，将与下面断路器路由配置中的 c.setName("slowcmd") 对应
     */
    @Bean
    public Customizer<ReactiveResilience4JCircuitBreakerFactory> customizerCircuitBreakerFactory() {
        log.info("--- 自定义名称为 slowcmd 的断路器配置 ---");
        // 此处设置断路器的超时等待时间 2 秒
        return f -> f.getTimeLimiterRegistry()
                .addConfiguration("slowcmd",
                        TimeLimiterConfig.custom().timeoutDuration(Duration.ofSeconds(2)).build());
    }
}
