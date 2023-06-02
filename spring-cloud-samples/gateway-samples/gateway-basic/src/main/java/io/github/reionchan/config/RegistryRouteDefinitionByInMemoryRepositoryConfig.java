package io.github.reionchan.config;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.cloud.gateway.config.GatewayAutoConfiguration;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.DispatcherHandler;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;

import static io.github.reionchan.GatewayBasicBootstrap.ROUTE_URI;

/**
 * 向 InMemoryRouteDefinitionRepository 注册自定义 RouteDefinition
 *
 * @author Reion
 * @date 2023-05-25
 **/
@Configuration
@CommonsLog
public class RegistryRouteDefinitionByInMemoryRepositoryConfig {

    /**
     * 基于 {@link InMemoryRouteDefinitionRepository} 路由定义仓库存储自定义 RouteDefinition
     * 只能用于当前网关，不能在不同网关之间共享，如果需要共享，请使用 {@link RedisRouteDefinitionRepository}
     *
     * <pre>
     * 原理：
     *      {@link InMemoryRouteDefinitionRepository} 继承 {@link RouteDefinitionLocator}
     *      而 {@link GatewayAutoConfiguration#routeDefinitionLocator(List)} 将所有 RouteDefinitionLocator 类型 Bean 收集，
     *      整合成 {@link CompositeRouteDefinitionLocator}，
     *      再由 {@link GatewayAutoConfiguration#routeDefinitionRouteLocator} 整合为 {@link RouteDefinitionRouteLocator}，
     *      再由 {@link GatewayAutoConfiguration#cachedCompositeRouteLocator(List)} 将所有 RouteLocator 类型 Bean 收集，
     *      整合成 {@link CachingRouteLocator}，
     *      最终被纳入 {@link GatewayAutoConfiguration#routePredicateHandlerMapping} 路由断言处理映射器的属性
     *      当客户端请求时，由 {@link DispatcherHandler} 交给此路由断言处理映射器进行断言匹配路由到所配置的 URI
     * </pre>
     */
    @Bean
    public InMemoryRouteDefinitionRepository inMemoryRouteDefinition() {
        log.info("--- 自定义路由方式一：使用 InMemoryRouteDefinitionRepository ---");
        InMemoryRouteDefinitionRepository memoryRouteDefinition = new InMemoryRouteDefinitionRepository();
        // 路由定义实例
        RouteDefinition routeDefinition = new RouteDefinition();
        routeDefinition.setId("path_route_user_agent");
        // 配置路由断言定义
        routeDefinition.setPredicates(List.of(new PredicateDefinition("Path=/user-agent")));
        routeDefinition.setUri(URI.create(ROUTE_URI));

        // 将路由定义实例保存到内存仓库
        memoryRouteDefinition.save(Mono.just(routeDefinition)).subscribe();
        return memoryRouteDefinition;
    }
}
