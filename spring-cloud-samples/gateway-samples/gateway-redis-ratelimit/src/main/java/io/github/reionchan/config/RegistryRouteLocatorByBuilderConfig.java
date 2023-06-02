package io.github.reionchan.config;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.cloud.gateway.config.GatewayAutoConfiguration;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.CachingRouteLocator;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.DispatcherHandler;

import java.util.List;

import static io.github.reionchan.GatewayRedisRateLimitBootstrap.ROUTE_URI;

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
        log.info("--- 自定义根据登录用户名为标记进行单个用户请求控频的路由 ---");
        return builder.routes()

                // 根据登录认证用户唯一标识 principle().getName() （依赖 Spring Security） 记录用户请求频率（记录在 Redis）
                // 当此用户请求超过设定频率时，将会返回 429 Too many requests
                .route("limit_route", r -> r
                        .host("*.limited.org").and().path("/anything/**")
                        .filters(f -> f.requestRateLimiter(c -> c.setRateLimiter(redisRateLimiter())))
                        .uri(ROUTE_URI))
                .build();
    }

    /**
     * 设置基于 Redis 的漏斗限流
     */
    @Bean
    RedisRateLimiter redisRateLimiter() {
        log.info("--- 定义基于 Redis 的漏斗控频器 RedisRateLimiter ---");
        // 每秒向漏斗添加 1 个令牌，漏斗最大 10 个令牌数，每次请求消耗 10 个令牌
        // 即：每 10 秒允许单个用户请求一次
        return new RedisRateLimiter(1, 10, 10);
    }
}
