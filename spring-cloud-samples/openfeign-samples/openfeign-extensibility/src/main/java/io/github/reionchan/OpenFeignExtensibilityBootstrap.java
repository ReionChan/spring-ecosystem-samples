package io.github.reionchan;

import feign.*;
import feign.micrometer.MicrometerObservationCapability;
import io.github.reionchan.client.FooClient;
import io.github.reionchan.response.WebResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignBuilderCustomizer;
import org.springframework.cloud.openfeign.FeignClientFactory;
import org.springframework.cloud.openfeign.FeignClientFactoryBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.ResponseEntity;
import outside.scan.config.FooClientConfiguration;

import java.lang.reflect.Type;

/**
 * OpenFeign 基础启动器
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
 *      使项目获得自动装配 Feign 功能
 *  2.4 引人依赖：feign-micrometer
 *      使项目获得对 Feign 客户端的可观察能力
 *
 * ============ Spring Cloud Open Feign 扩展能力 =================
 *
 * Feign 除具备重要组件接口具备不同实现的灵活能力外，在程序中
 * 也预留了几处可编程扩展的钩子，这些扩展点可以被用来扩展 Feign 功能。
 *
 * 1. {@link FeignClientFactoryBean#getTarget() getTarget()} 中 {@link FeignClientFactoryBean#applyBuildCustomizers(FeignClientFactory, Feign.Builder) applyBuildCustomizers(FeignClientFactory, Feign.Builder)}
 *  方法抽象出的 {@link FeignBuilderCustomizer} 对 {@link Feign.Builder} 扩展能力。
 *  示例可以参考 3.6.2 openfeign-loadbalancer 示例中，利用该定制类配置 Logger.Level
 *
 * 2. {@link Feign.Builder#build() build()} 中 {@link BaseBuilder#enrich() enrich()} 方法
 *  利用反射机制将 {@link BaseBuilder} 中定义的 Feign 组件进行装饰，来达到功能增强。
 *  而这些装饰类有一个通用编程接口抽象 {@link Capability}。
 *  Feign 本身利用该扩展能力实现对 Micrometer 的支持。
 *
 *  本示例演示通过 {@link Capability} 接口实现类 {@link MicrometerObservationCapability}
 *  的方法 {@link MicrometerObservationCapability#enrich(Client) enrich(Client)} 包装 {@link Client} 使其具备可观察能力。
 *  在 {@link outside.scan.config.FooClientConfiguration#micrometerObservationCapability micrometerObservationCapability()} 方法中添加
 *  针对 {@link FooClient} 客户端的观察处理器 {@link FooClientConfiguration.FooClientObservationHandler}，
 *  从而在 /actuator/metrics 端点中显示对该客户端方法的调用统计指标。
 *
 *
 * 3. {@link SynchronousMethodHandler#executeAndDecode(RequestTemplate, Request.Options) executeAndDecode()} 中 {@link SynchronousMethodHandler#targetRequest(RequestTemplate) targetRequest(RequestTemplate)}
 *  方法对 {@link RequestTemplate} 运用拦截器链 {@link RequestInterceptor} 对请求进行各种定制及功能实现。
 *  Feign 本身利用该扩展能力实现对请求、响应的压缩、OAuth2 访问令牌传递等功能
 *
 * 4. {@link ResponseHandler#handleResponse(String, Response, Type, long) handleResponse()} 中 {@link ResponseHandler#decode(Response, Type) decode(Response, Type)}
 *  方法将响应交给抽象的 {@link ResponseInterceptor} 响应拦截器进行处理，实现对响应的编程处理。
 *  Feign 默认实现为方法引用 {@link ResponseInterceptor#DEFAULT}，即 {@link InvocationContext#proceed()} 方法
 *
 * </pre>
 *
 * @author Reion
 * @date 2023-09-10
 **/
@Slf4j
@SpringBootApplication
// basePackageClasses 属性用来指定 Feign 客户端接口扫描路径
@EnableFeignClients(basePackageClasses = FooClient.class)
public class OpenFeignExtensibilityBootstrap {

    public static void main(String[] args) throws InterruptedException {
        ConfigurableApplicationContext context = SpringApplication.run(OpenFeignExtensibilityBootstrap.class, args);
        // 获取 FooClient Bean 实例，利用它调用 foo-service 的 API 接口，
        // 访问如下链接，可显示对 FooClient#pathVar 方法调用次数、执行耗时指标信息:
        //    <http://localhost:8080/actuator/metrics/fooClient.pathVar.count>
        //    <http://localhost:8080/actuator/metrics/fooClient.pathVar.time>
        //
        FooClient fooClient = context.getBean(FooClient.class);
        log.info("=== 获得 FooClient Bean 对象 ===\n{}", fooClient);
        ResponseEntity<WebResponse<?>> pathResp = fooClient.pathVar(1);
        log.info("=== Path 参数返回消息 ===\n{}", pathResp.getBody());
    }
}
