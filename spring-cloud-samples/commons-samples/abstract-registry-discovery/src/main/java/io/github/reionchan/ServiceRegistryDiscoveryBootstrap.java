package io.github.reionchan;

import com.alibaba.cloud.nacos.registry.NacosServiceRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.cloud.client.CommonsClientAutoConfiguration;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.actuator.HasFeatures;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.discovery.EnableDiscoveryClientImportSelector;
import org.springframework.cloud.client.discovery.composite.CompositeDiscoveryClient;
import org.springframework.cloud.client.discovery.composite.CompositeDiscoveryClientAutoConfiguration;
import org.springframework.cloud.client.discovery.composite.reactive.ReactiveCompositeDiscoveryClientAutoConfiguration;
import org.springframework.cloud.client.discovery.event.InstancePreRegisteredEvent;
import org.springframework.cloud.client.discovery.event.InstanceRegisteredEvent;
import org.springframework.cloud.client.discovery.simple.SimpleDiscoveryClientAutoConfiguration;
import org.springframework.cloud.client.discovery.simple.SimpleDiscoveryProperties;
import org.springframework.cloud.client.discovery.simple.reactive.SimpleReactiveDiscoveryClientAutoConfiguration;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.serviceregistry.*;
import org.springframework.cloud.configuration.CompatibilityVerifierAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.List;
import java.util.Map;

/**
 * 服务注册与发现启动器
 *
 * <pre>
 * 1. 服务注册与发现启用方式
 *    1.1 Spring Cloud 2022 版本之前，通过组合注解 @SpringCloudApplication 中的元注解 @EnableDiscoveryClient
 *        可以激活服务自动注册与服务发现客户端特性。其原理是 {@link EnableDiscoveryClient} 注解 @Import 引入了
 *        {@link EnableDiscoveryClientImportSelector}，由它将服务自动注册配置类 {@link AutoServiceRegistrationConfiguration}
 *        引入容器进行装配。
 *
 *    1.2 Spring Cloud 2022 版本开始，已删除 @SpringCloudApplication 注解，同时无需显式注解 @EnableDiscoveryClient，
 *        而采用自动装配机制由 spring-cloud-commons 包中的自动装配文件
 *          org.springframework.boot.autoconfigure.AutoConfiguration.imports
 *        引入 {@link AutoServiceRegistrationAutoConfiguration} 服务注册自动装配类根据
 *          spring.cloud.service-registry.auto-registration.enabled=true （默认）
 *        的自动装配条件，引人 {@link AutoServiceRegistrationConfiguration} 完成自动服务注册装配。
 *
 *    1.3 服务发现客户端则完全由自动装配机制完成，在 org.springframework.boot.autoconfigure.AutoConfiguration.imports 引入
 *        阻塞式的 {@link CompositeDiscoveryClientAutoConfiguration} {@link SimpleDiscoveryClientAutoConfiguration}
 *        响应式的 {@link ReactiveCompositeDiscoveryClientAutoConfiguration} {@link SimpleReactiveDiscoveryClientAutoConfiguration}
 *        分别用来配置组合式的服务发现客户端及基于简单的属性配置文件的服务发现客户端
 *
 * 2. 自动服务注册原理
 *    2.1 当服务注册与发现为启用时，即 spring.cloud.service-registry.auto-registration.enabled=true （默认）
 *
 *    2.2 由 1.2 自动装配类 {@link AutoServiceRegistrationAutoConfiguration} 会尝试自动绑定
 *        {@link AutoServiceRegistration} 自动服务注册接口的实现类，默认情况，未发现实现类时采取静默处理，不抛出异常。
 *          spring.cloud.service-registry.auto-registration.failFast=false (默认)
 *        设置为 true 时，当前应用就必须依赖一个服务注册客户端实现（Eureka、Consul、Nacos 或其他），否则将抛出异常，应用启动失败。
 *        本示例使用 Nacos 作为服务注册客户端实现。
 *
 *    2.3 Spring Cloud 给出了基于 {@link WebServerInitializedEvent} 事件监听机制的自动服务注册抽象类 {@link AbstractAutoServiceRegistration}
 *        来提供自动注册模版方法实现，其自动注册时机为应用的 WebServer 初始化完成后，开始向服务发现服务器注册当前应用。
 *        而该抽象规定了注册使用的注册器接口 {@link ServiceRegistry}，所有具体的注册器实现都将实现该接口
 *        本示例使用 Nacos 的实现类 {@link NacosServiceRegistry}
 *
 *        具体参考：{@link AbstractAutoServiceRegistration#onApplicationEvent(WebServerInitializedEvent)}
 *
 *    2.4 在自动注册的前后，分别会发布 {@link InstancePreRegisteredEvent} 实例注册前事件、{@link InstanceRegisteredEvent}  实例注册后事件。
 *
 * 3. Actuator 端点
 *      3.1 serviceregistry 服务注册端点
 *          GET /serviceregistry 显示服务注册客户端状态
 *          POST /serviceregistry 可以更改服务注册客户端状态
 *
 *      3.2 features 特性端点，显示当前应该已启用的特性与未启用的特性
 *          特性端点由自动装配类 {@link CommonsClientAutoConfiguration.ActuatorConfiguration#featuresEndpoint()} 定义，
 *          它收集容器中的所有 {@link HasFeatures} 类型的 Bean，其中 Spring Cloud Commons 中的定义的特性 Bean 包含：
 *          {@link DiscoveryClient} 服务发现客户端特性、{@link LoadBalancerClient} 负载均衡客户端特性
 *          它们在自动装配类 {@link CommonsClientAutoConfiguration.DiscoveryLoadBalancerConfiguration#commonsFeatures()} 被定义
 *
 *          当指定的特性类型，在容器中由具体实例对象 Bean，将会添加到已启用特性里，同时根据 {@link Class} 获得 {@link Package} 包中关于
 *          该特性的版本、提供商信息；同理，当容器中有未被装载的特性 Bean，将会添加到未启用特性里，仅包含特性名称信息。
 *          由于本示例未引入 Spring Cloud Loadbalancer 依赖，故 Spring Cloud Commons 中定义的 {@link LoadBalancerClient} 抽象接口特性
 *          未被装载到当前容器，所以此特性被放入未启用特性里，而 {@link DiscoveryClient} 显示为 {@link CompositeDiscoveryClient} 类型
 *
 *          {@code
 *              {
 *                  "type": "org.springframework.cloud.client.discovery.composite.CompositeDiscoveryClient",
 *                  "name": "DiscoveryClient",
 *                  "version": "4.0.2",
 *                  "vendor": "Pivotal Software, Inc."
 *              }
 *          }
 * 4. Spring Cloud Compatibility Verification 兼容性验证器
 *      可以通过 application.yaml 文件开启兼容性验证器
 *          spring.cloud.compatibility-verifier.enabled=true (默认 false，不启用)
 *      同时，还可以指定自定义的 Spring Boot 兼容版本
 *          spring.cloud.compatibility-verifier.compatible-boot-versions="3.0.x", "3.1.x" (此为 Spring Cloud 2022.0.2 默认)
 *      可以尝试去除本项目依赖的 Spring Boot 版本 3.0.x，让其只兼容 3.1.x，启动本项目将报出异常。
 *
 *      实现原理：参考自动装配类 {@link CompatibilityVerifierAutoConfiguration}
 *
 * </pre>
 *
 * @author Reion
 * @date 2023-06-15
 **/
@Slf4j
@SpringBootApplication
public class ServiceRegistryDiscoveryBootstrap {
    public static void main(String[] args) throws InterruptedException {
        ConfigurableApplicationContext context = SpringApplication.run(ServiceRegistryDiscoveryBootstrap.class, args);

        //1. 获取自动装载的服务发现客户端
        //1.1 容器中获取使用属性配置 spring.cloud.discovery.client.simple.instances 为前缀的手动配置的服务
        //    手动配置请查看本项目的 application.yaml 文件
        SimpleDiscoveryProperties discoveryProperties = context.getBean(SimpleDiscoveryProperties.class);
        log.info("=== 本机 service: \n{}", discoveryProperties.getLocal().getUri());
        StringBuffer strBuf = new StringBuffer("=== 手动设置的 services: \n");
        for (Map.Entry<String, List<DefaultServiceInstance>> entry : discoveryProperties.getInstances().entrySet()) {
            strBuf.append(entry.getKey() + " :\n");
            for (DefaultServiceInstance instance : entry.getValue()) {
                strBuf.append("\t" + instance.getUri() + "\n");
            }
        }
        log.info(strBuf.toString());

        //1.2 DiscoveryClient 接口，默认容器包含 CompositeDiscoveryClient 组合发现客户端
        //    它将所有容器中所有实现 DiscoveryClient 接口的发现客户端汇聚起来，在没有引入其它第三方的服务发现客户端实现（Eureka、Consul、Nacos）时
        //    默认仅包含支持本地属性配置文件设置的简单服务发现客户端 SimpleDiscoveryClient
        DiscoveryClient discoveryClient = context.getBean(DiscoveryClient.class);
        log.info("=== DiscoveryClient actual class: {}", discoveryClient.getClass());
        if (discoveryClient instanceof CompositeDiscoveryClient compoClient) {
            int i = 0;
            StringBuffer strBuffer = new StringBuffer("=== CompositeDiscoveryClient contains client list :\n");
            for (DiscoveryClient client : compoClient.getDiscoveryClients()) {
                strBuffer.append("\n").append(++i).append(". " + client.getClass().getSimpleName() + " services: \n");
                if (client.getServices().size() > 0) {
                    for (String service :  client.getServices()) {
                        strBuffer.append("\t" + service + "\n");
                        for (ServiceInstance instance : client.getInstances(service)) {
                            strBuffer.append("\t\t" + instance.getUri() + "\n");
                        }
                    }
                } else {
                    strBuffer.append("\t-- NO SERVICES --\n");
                }
            }
            log.info(strBuffer.toString());
        }

        //2. 服务自动注册 (本示例引入 Nacos 服务自动发现及注册客户端)
        //   本服务启动时，在 WebServerInitializedEvent 事件发生时，已将本服务通过 AutoServiceRegistration 注册到 Nacos 服务器
        AutoServiceRegistration registration = context.getBean(AutoServiceRegistration.class);
        log.info("=== AutoServiceRegistration 的实现类：{}", registration.getClass().getSimpleName());
        ServiceRegistry registry = context.getBean(ServiceRegistry.class);
        log.info("=== ServiceRegistry 的实现类：{}", registry.getClass().getSimpleName());
    }
}
