package io.github.reionchan;

import io.github.reionchan.config.RouteConfig;
import io.github.reionchan.config.SpringDocConfig;
import io.github.reionchan.controller.ServerController;
import io.github.reionchan.exception.GlobalExceptionHandler;
import io.github.reionchan.response.WebResponse;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.discovery.DiscoveryClientRouteDefinitionLocator;
import org.springframework.cloud.gateway.discovery.GatewayDiscoveryClientAutoConfiguration;
import org.springframework.cloud.gateway.filter.factory.RewritePathGatewayFilterFactory;
import org.springframework.cloud.gateway.handler.predicate.PathRoutePredicateFactory;

/**
 * 网关集成 SpringDoc 文档服务启动器
 *
 * <pre>
 *  由于网关集成服务发现、服务路由功能，能够为内部服务集群服务提供统一的访问地址（反向代理）。
 *  因此，可以在网关上路由下游所有微服务的 API 文档地址，提供统一的文档入口地址。
 *
 *  思路：
 *      1. 在网关配置下游所有微服务名称 (name) 与路径地址 (url) 对应关系。
 *          1.1 网关 application.yaml 配置中设置 springdoc.swagger-ui.urls 文档链接列表
 *              列表单个元素包含 name、url 属性设置
 *          1.2 name 为 Swagger 文档页中对应的每个服务的显示名称
 *          1.3 url 设置方便网关做路由转发的格式，如本例中：
 *              /v3/api-docs/foo-service
 *              ^^^^^^^^^^^^ ^^^^^^^^^^^
 *              路由前缀匹配   下游服务名称 serviceId
 *      2. 配置 API 文档路由规则
 *          2.1 网关 application.yaml 配置路由 spring.cloud.gateway.routes
 *          2.2 路径匹配 Path=/v3/api-docs/**
 *          2.3 路径重写 RewritePath=/v3/api-docs/(?<path>.*), /$\{path}/v3/api-docs
 *              例如：/v3/api-docs/foo-service
 *              将被重写成：http://gateway-ip:port/foo-service/v3/api-docs
 *          2.4 由于网关服务发现的默认负载均衡路由规则 (重写目的 URI 变为 lb://foo-service)
 *              路径重写：'/foo-service/?(?<remaining>.*)' '/${remaining}
 *              路径负载 URL：lb://foo-service
 *              所以，http://gateway-ip:port/foo-service/v3/api-docs
 *              将被定向到：lb://foo-service/v3/api-docs
 *              最后交给负载均衡客户端过滤器解析 lb://foo-service 根据负载均衡算法分配具体 foo-service 服务的 ip、端口
 *              最终转换为：http://ip:port/v3/api-docs 渲染 foo-service 的 API 文档首页
 *
 *              原理：{@link GatewayDiscoveryClientAutoConfiguration}
 *                  1. {@link GatewayDiscoveryClientAutoConfiguration#initPredicates()}
 *                      定义一个路径匹配断言 {@link PathRoutePredicateFactory}：
 *                          pattern         '/'+serviceId+'/**'
 *                  2. {@link GatewayDiscoveryClientAutoConfiguration#initFilters()}
 *                      定义一个路径重写过滤器 {@link RewritePathGatewayFilterFactory}：
 *                          regexp:         '/' + serviceId + '/?(?<remaining>.*)'
 *                          replacement:    '/${remaining}'
 *                  3. {@link GatewayDiscoveryClientAutoConfiguration#discoveryLocatorProperties()}
 *                      将1、2 路径匹配断言、过滤器设置到默认的服务发现定位属性配置中，交由下面 4 步骤完成服务发现客户端路由定义
 *                      此外，它还定义了默认的路由的目的 URI 格式 {@code urlExpression}：
 *                          'lb://'+serviceId
 *                  4. {@link DiscoveryClientRouteDefinitionLocator#getRouteDefinitions()}
 *                      它将上面定义的断言、过滤器中表达式中的 springEL 表达式变量 serviceId 替换为正在的服务名称
 *                      最终形成一条具体的服务实例的负载均衡路由规则：
 *                      该方法生成一条路由规则：
 *                          将【/foo-service/bar-path】定向到【lb://foo-service/bar-path】
 *
 *  实现：
 *      1. 参考 3.1.4 Gateway 集成 Nacos，先完成 Gateway 与 Nacos 集成
 *      2. 由于网关是 WebFlux 实现，故引人 SpringDoc 依赖：springdoc-openapi-starter-webflux-ui
 *      3. 提取出文档配置及分布式服务通用配置包 commons，它包含如下通用设置：
 *          3.1 WebMvc 实现下的 SpringDoc 依赖：springdoc-openapi-starter-webmvc-ui
 *          3.2 SpringDoc 公用配置类 {@link SpringDocConfig}（网关地址、版本、作者、响应通用组件设置等等）
 *          3.3 通用的请求端点 {@link ServerController} (提供服务器信息、服务条款)
 *          3.4 通用全局异常处理器 {@link GlobalExceptionHandler}
 *          3.5 统一 Web 请求响应格式 {@link WebResponse}
 *      4. 网关跨域配置 {@link RouteConfig}，解决 Swagger UI 文档中 API 调用测试时跨域报错问题
 *      5. 编写网关启动器 {@link GatewaySpringDocNacosBootstrap}
 *      6. 网关引入 commons 通用包依赖，并且剔除 SpringDoc 的 springdoc-openapi-starter-webmvc-ui 依赖（网关是 WebFlux 实现）
 *      7. 创建一个用来进行测试分布式文档的服务 foo-service 项目
 *          7.1 类似的，它也需要引人 Nacos 服务注册客户端、服务发现客户端
 *          7.2 引入 commons 通用包依赖，获得通用设置能力
 *          7.3 编写带有 OpenAPI 文档注解的 {@literal  io.github.reionchan.controller.GetController}
 *          7.4 编写带有 OpenAPI 文档注解的 {@literal  io.github.reionchan.controller.PostController}
 *          7.5 编写请求需要用到的值对象 {@literal io.github.reionchan.vo.RequestVo}
 *          7.6 编写启动器 {@literal io.github.reionchan.FooBootstrap}
 *
 *       8. 启动 nacos 服务
 *       9. 启动 foo-service 服务，可以启动多个不同端口的实例，测试文档的负载均衡
 *       10. 启动网关服务
 *       11. 浏览 API 文档地址，即网关下的 Swagger UI 地址：http://localhost:8080/swagger-ui.html
 *          11.1 文档路径参考配置属性 springdoc.swagger-ui.path
 *          11.2 页面右上角可以选择切换不同服务的 API 文档页（本样例只包含 gateway、foo 两个服务的文档页）
 *          11.3 可以发现，具体的 API 都是按照 Controller tag、方法名的字母排序
 *               这归功于 springdoc.swagger-ui.operations-sorter、springdoc.swagger-ui.tags-sorter 的配置
 *
 * </pre>
 *
 * @author Reion
 * @date 2023-06-05
 **/
@SpringBootApplication
public class GatewaySpringDocNacosBootstrap {
    public static void main(String[] args) {
        SpringApplication.run(GatewaySpringDocNacosBootstrap.class, args);
    }
}
