package io.github.reionchan.config;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.cloud.gateway.config.GatewayAutoConfiguration;
import org.springframework.cloud.gateway.route.CachingRouteLocator;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.DispatcherHandler;

import java.util.List;

import static io.github.reionchan.GatewayBasicBootstrap.ROUTE_URI;

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
        log.info("--- 自定义路由方式二：使用 RouteLocatorBuilder ---");
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
                .build();
    }
}
