package io.github.reionchan;

import feign.*;
import feign.hc5.ApacheHttp5Client;
import io.github.reionchan.client.FooClient;
import io.github.reionchan.client.HttpBinClient;
import io.github.reionchan.response.WebResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.cloud.openfeign.*;
import org.springframework.cloud.openfeign.loadbalancer.FeignBlockingLoadBalancerClient;
import org.springframework.cloud.openfeign.loadbalancer.FeignLoadBalancerAutoConfiguration;
import org.springframework.cloud.openfeign.loadbalancer.RetryableFeignBlockingLoadBalancerClient;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.InvocationHandler;
import java.net.HttpURLConnection;

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
 *      将使得 Feign 客户端具备负载均衡的能力
 *      当不具备负载均衡能力时，Feign 客户端必须显式指定 url 参数
 *  2.3 引入依赖：spring-cloud-starter-openfeign
 *      使项目获得自动装配 Feign 功能
 *  2.4 引入依赖：feign-okhttp
 *      使用 OkHttp 替换项目中默认的 Java {@link HttpURLConnection}
 *      当做 Http 连接客户端
 *      激活条件：
 *          1. 类路径存在 {@link feign.okhttp.OkHttpClient}
 *          2. spring.cloud.openfeign.okhttp.enabled=true
 *          3. 存在 {@link LoadBalancerClient}、{@link LoadBalancerClientFactory} Bean
 *      原理：
 *          {@link org.springframework.cloud.openfeign.loadbalancer.OkHttpFeignLoadBalancerConfiguration OkHttpFeignLoadBalancerConfiguration} 自动装配条件
 *  2.5 引入依赖：feign-hc5
 *      使用 HttpClient 当做 Http 连接客户端
 *      激活条件：
 *          1. 类路径存在 {@link ApacheHttp5Client}
 *          2. spring.cloud.openfeign.httpclient.hc5.enabled=true 或不配置
 *          3. 存在 {@link LoadBalancerClient}、{@link LoadBalancerClientFactory} Bean
 *      当与 feign-okhttp 同时存在且可被激活时，优先 OkHttp
 *      原理：
 *          优先级基于 {@link FeignLoadBalancerAutoConfiguration} 上的
 *          Http 负载均衡客户端配置 @Import 顺序
 *
 * ============ Spring Cloud OpenFeign 运作原理 ==============
 *
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
 *          当 {@link okhttp3.OkHttpClient} 客户端被引入时，
 *          将被封装为 {@link Client} 实现类 {@link feign.okhttp.OkHttpClient}
 *      2. {@link org.springframework.cloud.openfeign.loadbalancer.HttpClient5FeignLoadBalancerConfiguration HttpClient5FeignLoadBalancerConfiguration}
 *          当 {@link org.apache.hc.client5.http.classic.HttpClient HttpClient} 客户端被引入时，
 *          将被封装为 {@link Client} 实现类 {@link feign.hc5.ApacheHttp5Client}
 *       三种 {@link Client} 实现类装载优先级：
 *       OkHttpClient > ApacheHttp5Client > Client.Default
 *
 *  ============ Spring Cloud OpenFeign 支持负载均衡原理 =============
 *
 *  根据上面运行原理的 4.9 小节描述可知，OpenFeign 定义了 Http 连接客户端接口
 *  {@link Client}，除默认实现 {@link feign.Client.Default} 外还引入了：
 *  1. {@link feign.okhttp.OkHttpClient}
 *      由 feign-okhttp 引入，它对 {@link okhttp3.OkHttpClient} 进行装饰适配
 *  2. {@link ApacheHttp5Client}
 *      由 feign-hc5 引入，它对 HttpClient 进行装饰适配
 *  3. {@link FeignBlockingLoadBalancerClient}
 *      由 spring-cloud-openfeign-core 引入
 *      它采用装饰模式设计，可以对 {@link Client} 实例进行装饰，
 *      使其具备负载均衡的能力
 *  4. {@link RetryableFeignBlockingLoadBalancerClient}
 *      由 spring-cloud-openfeign-core 引入
 *      它也采用装饰模式设计，可以对 {@link Client} 实例进行装饰，
 *      使其具备可重试的负载均衡能力
 *
 *  由此可以看出，最后两个具备负载均衡能力的实现，它们是 OpenFeign 具备
 *  负载均衡的根本原因，而且他们通过对抽象接口 {@link Client} 进行包装
 *  从而可以灵活在 {@link feign.Client.Default}、{@link feign.okhttp.OkHttpClient}、
 *  {@link ApacheHttp5Client} 等不同的 Http 连接客户端切换。
 *
 *  目前自动装配类 {@link FeignLoadBalancerAutoConfiguration} 会在
 *  {@link Import @Import} 中根据激活及优先顺序默认注册一个具备负载均衡能力的客户端，
 *  当然最后 Feign 客户端执行时是否使用到负载均衡还要取决于 Feign 客户端
 *  是否配置了 URL 属性，如果有配置将会提取负载均衡能力客户端中的被包装的
 *  {@link Client} 直接发送请求调用。
 *  原理参考：
 *      {@link FeignClientFactoryBean#getTarget() getTarget()} 方法中对 url 是否为空的判断
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
public class OpenFeignLoadbalancerBootstrap {

    /**
     * <pre>
     * 采用 {@link FeignBuilderCustomizer} 对容器中的 Feign.Builder Bean 进行定制
     *
     * 原理：
     *    程序在配置过程中预留的两处执行 {@link FeignBuilderCustomizer} 定制器的钩子
     *    1. {@link FeignClientFactoryBean#loadBalance(feign.Feign.Builder, FeignClientFactory, feign.Target.HardCodedTarget) loadBalance()} 方法
     *    2. {@link FeignClientFactoryBean#getTarget() getTarget()} 方法
     *    这两处方法中都会执行 {@link FeignClientFactoryBean#applyBuildCustomizers(FeignClientFactory, Feign.Builder) applyBuildCustomizers()} 方法
     *    对 {@link Feign.Builder} 进行定制化
     * </pre>
     */
    @Bean
    public FeignBuilderCustomizer customizer() {
        return builder -> {
            builder.logLevel(Logger.Level.HEADERS);
        };
    }

    public static void main(String[] args) throws InterruptedException {
        SpringApplicationBuilder builder = new SpringApplicationBuilder(OpenFeignLoadbalancerBootstrap.class);
        // 关闭重试支持，防止下面获取的 Client 有变为 RetryableFeignBlockingLoadBalancerClient
        builder.properties("spring.cloud.loadbalancer.retry.enabled=false");
        // 1. 通过激活开关组合，来验证几种 Http 连接客户端装配优先级
        builder.properties(
               // 激活 okhttp
"spring.cloud.openfeign.okhttp.enabled=true",
               // 激活 httpclient
               "spring.cloud.openfeign.httpclient.hc5.enabled=true"
        );
        /* 2. 设置属性开启请求及响应的压缩
           值得注意的是，当 OkHttp 客户端被启用时，由于它具备【透明式】的压缩处理机制
           即：
            请求头没有 Accept-Encoding、响应头有 Content-Encoding=gzip、响应体有数据
            OkHttp 将会自动设置请求头及对压缩格式的响应体解压缩
           故即使下面这两个参数设置为 true，OkHttp 客户端启用时也不会在请求头
           设置 Content-Encoding、Accept-Encoding，转而交给 OkHttp 内部添加
           原理：
              FeignContentGzipEncodingAutoConfiguration、FeignAcceptGzipEncodingAutoConfiguration
              两个自动装配类中的装配条件：
                @Conditional(OkHttpFeignClientBeanMissingCondition.class)
        * */
        builder.properties(
                // 激活请求压缩
 "spring.cloud.openfeign.compression.request.enabled=true",
                // 激活响应压缩
                "spring.cloud.openfeign.compression.response.enabled=true"
        );
        ConfigurableApplicationContext context = builder.run(args);

        // 测试 Http 连接客户端装配优先级
        Environment env = context.getEnvironment();
        String okhttpEn = env.getProperty("spring.cloud.openfeign.okhttp.enabled");
        String httpclientEn = env.getProperty("spring.cloud.openfeign.httpclient.hc5.enabled");
        FeignBlockingLoadBalancerClient client = context.getBean(FeignBlockingLoadBalancerClient.class);
        Client delegate = client.getDelegate();
        String info = """
                \n
                ====== Client 装配优先级 =======
                okhttp 是否激活：{}
                httpclient 是否激活：{}
                Client 类型：{}
                启用客户端：{}
                ====== Client 装配优先级 =======
                """;
        log.info(info, okhttpEn, httpclientEn, client.getClass().getSimpleName(), delegate.getClass().getName());

        // 测试请求响应压缩
        HttpBinClient httpBinClient = context.getBean(HttpBinClient.class);
        ResponseEntity<String> gzipRes = httpBinClient.gzip();
        log.info("=== 请求响应压缩 ===\n{}", gzipRes.getBody());
    }
}
