package io.github.reionchan;

import io.github.reionchan.controller.EchoController;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Cloud Gateway 与 Nacos 集成实现服务发现、服务集群负载均衡示例
 *
 * 它是基于 Spring WebFlux 框架，采用 Reactive 非阻塞式编程实现的 API 网关。
 * 由于其是 Spring Cloud 生态体系成员，依托 Spring Cloud 的云原生特性，
 * 该网关对分布式集群的服务发现、负载均衡、服务熔断都有较好的支持。
 *
 * <pre>
 *  1. POM 中引入 spring-cloud-starter-gateway 依赖
 *
 *  2. POM 中引入 spring-cloud-starter-alibaba-nacos-discovery 依赖
 *      2.1 引人 spring-cloud-starter-loadbalancer 负载均衡通用
 *
 *  3. 定义网关启动器 {@link GatewayNacosBootstrap}
 *
 *  4. 编写控制器 {@link EchoController}
 *
 *  5. 编写单元测试 {@literal  GatewayNacosTest}
 *
 *  当启动端口不通的多个本网关服务时，请求多次下面 URL：
 *      http://localhost:8080/gateway-nacos/echoAppName
 *
 *  将会返回不同端口的结果，例如：
 *      gateway-nacos@8080
 *      gateway-nacos@8081
 *
 *  证明实现了网关自身的负载均衡
 *
 * </pre>
 *
 * @author Reion
 * @date 2023-05-25
 **/
@SpringBootApplication
public class GatewayNacosBootstrap {

    public static void main(String[] args) {
        SpringApplication.run(GatewayNacosBootstrap.class, args);
    }
}
