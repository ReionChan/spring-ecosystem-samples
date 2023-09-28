package io.github.reionchan;

import feign.Contract;
import feign.Feign;
import feign.Logger;
import feign.codec.Decoder;
import feign.codec.Encoder;
import io.github.reionchan.client.FooClient;
import io.github.reionchan.response.WebResponse;
import io.github.reionchan.vo.RequestVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.*;
import org.springframework.cloud.openfeign.clientconfig.FeignClientConfigurer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import outside.scan.config.FooClientConfiguration;

import java.util.Map;

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
 *
 * ============ 基本使用 ========================
 *
 * 3. 本示例启用了 FeignClient 功能
 *
 *  3.1 使用 {@link EnableFeignClients @EnableFeignClients} 激活
 *      注解常用属性方法：
 *          value|basePackages  Feign 客户端接口包扫描路径字符串
 *          basePackageClasses  Feign 客户端接口类来获取类的包扫描路径
 *          defaultConfiguration Feign 客户端全局默认配置类
 *          clients 直接指定 Feign 客户端类，设置后将不会进行扫描
 *
 *  3.2 编写 {@link FooClient} Feign 客户端接口调用 foo-service 服务
 *      客户端使用 {@link FeignClient @FeignClient} 类注解修饰
 *      该注解常用属性方法：
 *          value|name     服务名称，可以加协议前缀，可作为负载均衡的 serviceId
 *          contextId      设置该客户端 bean 名称，不设置时使用 value|name
 *          url            服务端绝对 URL 或可解析的主机名
 *          configuration  该客户端定制化 Java 配置类
 *          fallback|fallbackFactory   调用异常时补偿类或工厂类
 *          path        该客户端所有方法的路径映射通用路径前缀
 *          primary     当该客户端接口对应多个 bean 实例时（负载均衡多个 fallback）
 *                      标识某个为主 bean
 *          qualifiers  当该客户端接口对应多个 bean 实例时（负载均衡多个 fallback）
 *                      用做主 bean 之外的可选注入别名
 *
 *  3.3 编写 {@link FooClientConfiguration} 定制化 FooClient
 *      1. Feign 日志配置
 *          1.1 开启请求日志打印
 *              Feign 客户端只在该客户端日志等级设置为 DEBUG 时才会输出请求日志，
 *              每个客户端设置日志级别时，在 logging.level 属性后追加类全限定名即可，例如：
 *                  logging.level.foo.bar.FooClient=DEBUG
 *          1.2 请求日志打印级别 {@link Logger.Level}
 *              NONE    不输出任何请求日志
 *              BASIC   只打印请求方法、请求 URL 和响应码、执行时间
 *              HEADERS 打印请求及响应的头部、Body、元数据信息
 *              FULL    打印请求及响应的头部、Body、元数据及方法、URL、响应码等全部信息
 *      2. 请求处理配置
 *          针对单个客户端请求配置，可以在此定制化配置中定义 {@link feign.Request.Options} 类型的 Bean
 *          它可以设置连接超时时间、读取超时时间、是否允许重定向设置
 *
 * ============ Feign 客户端代理 Bean 加载时机 ========================
 *
 * 4. 默认 FeignClient Bean 是启动时就装在，可以在 application.yaml 设置懒加载：
 *      spring.cloud.openfeign.lazy-attributes-resolution=true
 *
 *  非懒加载时：
 *      服务启动时会先创建 {@link FeignClientFactoryBean} 工厂 Bean，
 *      真正用时，再通过此工厂 Bean 来创建动态代理类。
 *  懒加载时：
 *      服务启动时只会包含 lazyInit=true 标识的 {@link BeanDefinition} Bean 定义类，
 *      该 Bean 实际为 {@link FeignClientFactoryBean} 创建动态代理类的 lambda 表达式，
 *      真正用时，才会执行该表达式。
 *      具体动态代理创建，参考：{@link FeignClientFactoryBean#getObject()}
 *  原理：
 *      {@link org.springframework.cloud.openfeign.FeignClientsRegistrar#registerFeignClient(BeanDefinitionRegistry, AnnotationMetadata, Map) registerFeignClient() 方法}
 *
 * ============= 基本配置 ============================
 *
 * 5. Feign 客户端常用自定义配置
 *
 *  5.1 自定义配置方式
 *      1. Java 配置类
 *          1.1 {@link EnableFeignClients @EnableFeignClients} 属性 defaultConfiguration 方式
 *          1.2 {@link FeignClient @FeignClient} 属性 configuration 方式
 *      2. 属性文件配置
 *          2.1 {@link FeignClientProperties} 收集以 spring.cloud.openfeign.client 开头的属性配置
 *              并将其封装成以客户端名称为 Key,以 {@link FeignClientProperties.FeignClientConfiguration FeignClientConfiguration} 为 Value 的 Map
 *  5.2 配置优先级
 *      1. 默认情况
 *          1.1 不同配置方式
 *              【属性文件配置】 优先 【Java 配置】
 *          1.2 相同配置方式
 *              【属性文件配置】客户端名称匹配的属性配置 优先 名称为 "default" 的属性配置
 *              【Java 配置】客户端名称匹配的子上下文配置 优先 名称为 "default" 的子上下文配置
 *      2. 影响配置优先级
 *          2.1 覆盖默认 {@link FeignClientsConfiguration#feignClientConfigurer() FeignClientConfigurer} Bean 定义
 *              将其属性方法 {@link FeignClientConfigurer#inheritParentConfiguration() inheritParentConfiguration()} 返回 false
 *              这将导致【属性文件配置】全部失效，而客户端名称匹配的子上下文除
 *              {@link Encoder}、{@link Decoder}、{@link Contract} Bean 之外，都不继承父上下文的配置
 *
 *              原理：
 *              1. {@link FeignClientFactoryBean#configureFeign(FeignClientFactory, Feign.Builder) configureFeign()} 方法通过 {@link FeignClientConfigurer}
 *                  设置 inheritParentContext 属性，而该属性决定是否执行
 *                  {@link FeignClientFactoryBean#configureUsingProperties(FeignClientProperties.FeignClientConfiguration, Feign.Builder) configureUsingProperties()} 方法读取【属性配置文件】的配置
 *              2.{@link FeignClientFactoryBean#feign(FeignClientFactory) feign()} 方法对父类上下文 {@link Encoder}、{@link Decoder}、{@link Contract} 三个 Bean 的继承，
 *                  不受 inheritParentContext 属性影响
 *          2.2 设置【Java 配置】优先级高于【属性文件配置】
 *              将下面属性设置为 false 即可
 *                  spring.cloud.openfeign.client.defaultToProperties=false
 *
 *              原理：
 *              {@link FeignClientFactoryBean#configureFeign(FeignClientFactory, Feign.Builder) configureFeign()} 方法根据 defaultToProperties 决定执行
 *              {@link FeignClientFactoryBean#configureUsingProperties(FeignClientProperties.FeignClientConfiguration, Feign.Builder) configureUsingProperties()}、
 *              {@link FeignClientFactoryBean#configureUsingConfiguration(FeignClientFactory, Feign.Builder) configureUsingConfiguration()} 方法的顺序
 * </pre>
 *
 * @author Reion
 * @date 2023-09-10
 **/
@Slf4j
@SpringBootApplication
// basePackageClasses 属性用来指定 Feign 客户端接口扫描路径
@EnableFeignClients(basePackageClasses = FooClient.class)
public class OpenFeignBasicBootstrap {

    public static void main(String[] args) throws InterruptedException {
        ConfigurableApplicationContext context = SpringApplication.run(OpenFeignBasicBootstrap.class, args);
        // 1. 获取 FooClient Bean 实例，利用它调用 foo-service 的 API 接口，
        //    留意客户端不同注释声明对客户端最终 Http 请求的头、请求体的影响
        FooClient fooClient = context.getBean(FooClient.class);
        log.info("=== 获得 FooClient Bean 对象 ===\n{}", fooClient);
        ResponseEntity<WebResponse<?>> pathResp = fooClient.pathVar(1);
        log.info("=== Path 参数返回消息 ===\n{}", pathResp.getBody());
        ResponseEntity<WebResponse<?>> queryResp = fooClient.queryVar(2, "SunWuKong");
        log.info("=== Query 参数返回消息 ===\n{}", queryResp.getBody());
        RequestVo vo = RequestVo.builder().userName("ZhuWuNeng").age(30).build();
        ResponseEntity<WebResponse<?>> urlEncodedResp = fooClient.formUrlEncodedDataBody(vo);
        log.info("=== URL Encoded 参数返回消息 ===\n{}", urlEncodedResp.getBody());
        ResponseEntity<WebResponse<?>> formDataResp = fooClient.formDataBody(vo);
        log.info("=== Form Data 参数返回消息 ===\n{}", formDataResp.getBody());
        ResponseEntity<WebResponse<?>> jsonResp = fooClient.jsonBody(vo);
        log.info("=== Json 参数返回消息 ===\n{}", jsonResp.getBody());
        MultipartFile file = new MockMultipartFile("name", new byte[]{3, 2, 1});
        ResponseEntity<WebResponse<?>> multipartFileResp = fooClient.multipartFileBody(file, "fileNameValue.ext");
        log.info("=== Multipart File 参数返回消息 ===\n{}", multipartFileResp.getBody());

        /*
          2. 通过在 FooClientConfiguration 配置类、application.yaml 文件分别对 foo-service 客户端配置不同的日志等级
             来验证 inheritParentContext、defaultToProperties 对配置优先级的影响

            1. inheritParentContext 属性
                由本类中的 feignClientConfigurer() Bean 定义方法来设置
            2. defaultToProperties 属性
                由 application.yaml 中的属性 spring.cloud.openfeign.client.defaultToProperties 来设置
            3. 日志等级：
                属性文件中配置的为 BASIC，可以观察日志仅仅输出请求行、返回行信息
                配置类中配置的为 FULL，可以观察日志额外输出了请求头、响应头等信息
         */

    }

    /**
     * 覆盖默认 {@link FeignClientsConfiguration#feignClientConfigurer() FeignClientConfigurer} Bean 定义
     * 将其属性方法 {@link FeignClientConfigurer#inheritParentConfiguration() inheritParentConfiguration()} 返回 false
     * 这将导致：
     *  1. application.yaml 中关于 Feign 客户端的配置全部失效
     *  2. 客户端名称匹配的子上下文除 {@link Encoder}、{@link Decoder}、{@link Contract} Bean 之外，都不继承父上下文的配置
     */
    @Bean
    public FeignClientConfigurer feignClientConfigurer() {
        return new FeignClientConfigurer() {
            @Override
            public boolean inheritParentConfiguration() {
                // 请更改此设置，默认为 true，即：属性配置文件优先
                return true;
            }
        };
    }
}
