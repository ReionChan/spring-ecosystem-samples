package io.github.reionchan;

import feign.*;
import io.github.reionchan.client.HttpBinClient;
import io.github.reionchan.client.HttpBinClientFallback;
import io.github.reionchan.client.HttpBinClientFallbackFactory;
import io.github.reionchan.response.WebResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.openfeign.*;
import org.springframework.cloud.openfeign.loadbalancer.FeignLoadBalancerAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;

/**
 * OpenFeign 整合支持 Spring Cloud CircuitBreaker
 *
 * <pre>
 *
 * =========== 环境依赖 =======================
 *
 * 1. 本示例运行复用了 3.5 Gateway 样例中的通用组件：
 *  commons 通用组件（模块 3.5.5.1）
 *   包含统一的 Web API 返回格式类 {@link WebResponse}
 *  foo-service 服务（模块 3.5.5.2）
 *   提供给本示例的 OpenFeign 客户端调用的 Controller Web API
 *
 * 2. 本示例及 foo-service 服务都依赖 Nacos 的服务注册
 *  2.1 引入依赖：spring-cloud-starter-alibaba-nacos-discovery
 *      在 application.yaml 中配置 Nacos 服务地址
 *  2.2 引入依赖：spring-cloud-starter-loadbalancer
 *      将使得 Feign 客户端具备负载均衡的能力，在之后的示例详细讲解
 *  2.3 引入依赖：spring-cloud-starter-openfeign
 *      使项目获得自动装配 Feign 功能，其中已入两个依赖包：
 *      1. feign-core
 *          OpenFeign 核心包
 *      2. spring-cloud-openfeign-core
 *          Spring Cloud 对开源 OpenFeign 的整合，
 *          使后者支持 Spring 相关配置特性
 *  2.4 引入依赖：spring-cloud-starter-circuitbreaker-resilience4j
 *      使项目获得自动装配基于 Resilience4J 的 Spring Cloud CircuitBreaker
 *
 * ============ Spring Cloud OpenFeign 运作原理 ==============
 * 1. {@link EnableFeignClients @EnableFeignClients} 注解激活 Feign 客户端功能
 * 2. 其元注解 {@link Import @Import} 引入 Feign 客户端注册器 {@link org.springframework.cloud.openfeign.FeignClientsRegistrar FeignClientsRegistrar}
 * 3. 该注册器扫描搜集被 {@link FeignClient @FeignClient} 注解的接口，
 *    并将这些接口定义为 {@link FeignClientFactoryBean} 类型的 Bean
 * 4. 应用启动时（非懒加载）将通过这个工厂 Bean 生成接口的动态代理实例
 *  4.1 {@link FeignAutoConfiguration} 自动装配包含所有 Feign 客户端子上下文的
 *      命名上下文工厂 {@link FeignClientFactory}
 *  4.2 结合 {@link FeignClientFactory} 上下文配置生成 {@link Feign.Builder} 客户端配置类实例
 *  4.3 结合 {@link FeignClientFactory} 上下文获得代理目标选择器 {@link Targeter} 实例
 *  4.4 由 {@link Targeter} 的 target 方法获得最终的 Feign 客户端接口的动态代理类
 *  4.5 而所以 {@link Targeter} 实例最终都将委托 {@link Feign.Builder#target(Target) target(Target)} 方法构建代理类
 *  4.6 当前 {@link Feign.Builder#target(Target) target(Target)} 方法委托 {@link ReflectiveFeign} Java 动态代理获得代理类
 *  4.7 而生成的代理类把对 Feign 客户端接口定义的方法的调用通过 {@link InvocationHandler } 映射到
 *      {@link InvocationHandlerFactory.MethodHandler MethodHandler} 的 invoke 方法的调用
 *  4.8 {@link InvocationHandlerFactory.MethodHandler MethodHandler} 的实现类之一 {@link feign.SynchronousMethodHandler SynchronousMethodHandler} 的 {@link SynchronousMethodHandler#invoke(Object[]) invoke} 方法
 *      将请求 {@link Request} 交给 {@link Client} 去执行，最终获得 {@link Response} 结果
 *  4.9 {@link Client} 是对能够执行 HTTP 协议请求的客户端的抽象
 *      默认是由 Java 自带的 {@link HttpURLConnection} 来执行请求，对应的 {@link Client} 实现类为：
 *      {@link Client.Default}
 *      此外，自动装配类 {@link FeignLoadBalancerAutoConfiguration} 额外条件装配第三方客户端配置类：
 *      1. {@link org.springframework.cloud.openfeign.loadbalancer.OkHttpFeignLoadBalancerConfiguration OkHttpFeignLoadBalancerConfiguration}
 *          当 {@link okhttp3.OkHttpClient OkHttpClient} 客户端被引入，
 *          并且属性：spring.cloud.openfeign.okhttp.enabled 设置为 true 时，
 *          将被封装为 {@link Client} 实现类 {@link feign.okhttp.OkHttpClient}
 *      2. {@link org.springframework.cloud.openfeign.loadbalancer.HttpClient5FeignLoadBalancerConfiguration HttpClient5FeignLoadBalancerConfiguration}
 *          当 {@link org.apache.hc.client5.http.classic.HttpClient HttpClient} 客户端被引入时，
 *          其属性没有配置默认激活，故无需特意配置
 *          将被封装为 {@link Client} 实现类 {@link feign.hc5.ApacheHttp5Client}
 *       三种 {@link Client} 实现类装载优先级：
 *       OkHttpClient > ApacheHttp5Client > Client.Default
 *
 * ============ 支持 CircuitBreaker 原理 =======================
 * 1. Feign 的熔断器自动装配条件
 *  配置类 {@link FeignAutoConfiguration.CircuitBreakerPresentFeignTargeterConfiguration CircuitBreakerPresentFeignTargeterConfiguration} 的条件注解
 *  1.1 类路径中包含 {@link org.springframework.cloud.client.circuitbreaker.CircuitBreaker CircuitBreaker}
 *      该类为 Spring Cloud Commons 包中对熔断器的抽象接口，故一定包含
 *  1.2 属性 spring.cloud.openfeign.circuitbreaker.enabled=true
 *      在 application.yaml 中设置该属性值为 true
 *  1.3 容器中存在 {@link CircuitBreakerFactory} 类型的 Bean
 *      已经引入 spring-cloud-starter-circuitbreaker-resilience4j
 *      其中包含该类型的实现类的自动装配 Bean {@link Resilience4JCircuitBreakerFactory}
 *
 * 2. 自动装配支持熔断器的 {@link Feign.Builder} 实现 Bean {@link FeignCircuitBreaker.Builder}
 *  该实现类在自动装配类 {@link FeignClientsConfiguration.CircuitBreakerPresentFeignBuilderConfiguration CircuitBreakerPresentFeignBuilderConfiguration} 中装配
 *  该实现类覆盖默认的 {@link Feign.Builder}
 *
 * 3. 自动装载支持熔断器的 {@link Targeter} 实现 Bean {@link org.springframework.cloud.openfeign.FeignCircuitBreakerTargeter FeignCircuitBreakerTargeter}
 *  该实现类在自动装配类 {@link FeignAutoConfiguration.CircuitBreakerPresentFeignTargeterConfiguration CircuitBreakerPresentFeignTargeterConfiguration} 中装配
 *  该实现类覆盖默认的 {@link org.springframework.cloud.openfeign.DefaultTargeter DefaultTargeter}
 *  该实现类在构件时对步骤 2 的 {@link FeignCircuitBreaker.Builder} 实例的属性进行填充：
 *  3.1 circuitBreakerFactory 设置为 {@link Resilience4JCircuitBreakerFactory} 实例
 *  3.2 feignClientName 设置为 Feign 客户端的 name|contextId
 *  3.3 circuitBreakerGroupEnabled 设置为默认 false
 *      由属性 spring.cloud.openfeign.circuitbreaker.group.enabled 控制，默认 false
 *      原理参考：{@link FeignAutoConfiguration.CircuitBreakerPresentFeignTargeterConfiguration#circuitBreakerFeignTargeter(CircuitBreakerFactory, boolean, CircuitBreakerNameResolver) circuitBreakerFeignTargeter()} circuitBreakerGroupEnabled 参数自动注入
 *  3.4 circuitBreakerNameResolver 设置为默认 {@link FeignAutoConfiguration.CircuitBreakerPresentFeignTargeterConfiguration.AlphanumericCircuitBreakerNameResolver AlphanumericCircuitBreakerNameResolver}
 *      原理参考：{@link FeignAutoConfiguration.CircuitBreakerPresentFeignTargeterConfiguration#alphanumericCircuitBreakerNameResolver() alphanumericCircuitBreakerNameResolver()} Bean 配置方法
 *
 * 4. {@link FeignCircuitBreaker.Builder} 属性填充后，将构造 {@link org.springframework.cloud.openfeign.FeignCircuitBreakerInvocationHandler FeignCircuitBreakerInvocationHandler}
 *    对象的 lambda 表达式设置到继承于父类的属性 invocationHandlerFactory，从而将其传递到
 *    {@link ReflectiveFeign} 中的 factory 属性，使得 {@link org.springframework.cloud.openfeign.FeignCircuitBreakerInvocationHandler FeignCircuitBreakerInvocationHandler}
 *    成为 Java 动态代理的 {@link InvocationHandler} 具体实现，之后对 Feign 客户端接口方法
 *    的调用，对被委托给 {@link org.springframework.cloud.openfeign.FeignCircuitBreakerInvocationHandler FeignCircuitBreakerInvocationHandler} 处理
 *    而它将会创建熔断器，并将请求交给这个熔断器中执行。
 *    具体执行逻辑参考方法：{@link FeignCircuitBreakerInvocationHandler#invoke(Object, Method, Object[]) FeignCircuitBreakerInvocationHandler#invoke()}
 *
 * ============ 支持 CircuitBreaker 示例 =======================
 * 1. 编写 Feign 客户端类 {@link HttpBinClient}
 *  它能够访问 http://httpbin.org 的 /delay/{delay} 延迟响应端点
 *  用来验证熔断器超时熔断
 *
 * 2. 编写熔断时的回退类 {@link HttpBinClientFallback}
 *  它在发生熔断时用来顶替真实的响应结果
 *
 * 3. 编写熔断时的回退工厂类 {@link HttpBinClientFallbackFactory}
 *  它再发生熔断时可以获得熔断时的异常信息，并且能够动态创建不同的回退类
 *  当回退类、回退工厂类都在注解 @FeignClient 中指定，优先回退类
 *
 * 4. 采用属性 spring.cloud.openfeign.circuitbreaker.alphanumeric-ids=true
 *  来指定熔断器 ID 名称的生成策略为只包含字母与数字的组合
 *  详细参考 application.yaml 该属性的注释说明
 *
 * 5. 根据上面的熔断器 ID 名称生成策略来配置相应 Feign 客户端熔断器的参数设置
 *  例如：在本例中 application.yaml 配置名称 ID 为 HttpBinClientpathVarInteger
 *      的客户端的熔断超时时间为 3 秒，即：
 *      resilience4j.timelimiter.configs.HttpBinClientpathVarInteger.timeout-duration=3s
 *
 *
 * </pre>
 *
 * @author Reion
 * @date 2023-09-10
 **/
@Slf4j
@SpringBootApplication
@EnableFeignClients(basePackages = "io.github.reionchan.client")
public class OpenFeignCircuitBreakerBootstrap {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(OpenFeignCircuitBreakerBootstrap.class, args);
        HttpBinClient httpBinClient = context.getBean(HttpBinClient.class);
        log.info("=== 获得 HttpBinClient Bean 对象 ===\n{}", httpBinClient);
        // 请求响应延时 1 秒的接口时，由于小于熔断器超时的 3 秒，不发生熔断
        ResponseEntity<String> pathResp1Sec = httpBinClient.pathVar(1);
        log.info("=== 熔断器超时时间 3 秒，请求响应延时 1 秒时 ===\n{}", pathResp1Sec.getBody());
        // 请求响应延时 6 秒的接口时，由于大于熔断器超时的 3 秒，发生熔断
        // 并且根据 fallback 或 fallbackFactory 属性的配置，将返回回退信息
        ResponseEntity<String> pathResp6Sec = httpBinClient.pathVar(6);
        log.info("=== 熔断器超时时间 3 秒，请求响应延时 6 秒时 ===\n{}", pathResp6Sec.getBody());
    }
}
