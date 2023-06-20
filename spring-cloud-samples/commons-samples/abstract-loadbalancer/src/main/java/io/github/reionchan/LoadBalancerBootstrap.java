package io.github.reionchan;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.client.discovery.composite.CompositeDiscoveryClient;
import org.springframework.cloud.client.discovery.composite.reactive.ReactiveCompositeDiscoveryClient;
import org.springframework.cloud.client.discovery.simple.SimpleDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.*;
import org.springframework.cloud.client.loadbalancer.reactive.*;
import org.springframework.cloud.context.named.NamedContextFactory;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClientConfiguration;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.cloud.loadbalancer.blocking.client.BlockingLoadBalancerClient;
import org.springframework.cloud.loadbalancer.core.*;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.cloud.loadbalancer.support.LoadBalancerEagerContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import outside.scan.config.CustomLoadBalancerConfiguration;

/**
 * 负载均衡启动器
 *
 * <pre>
 * Spring Cloud 对负载均衡客户端的抽象与之前的服务注册、服务发现类似，
 * 都将抽象层纳入 spring-cloud-commons 模块之中。
 * 但是，负载均衡在项目结构上，与服务注册、服务发现略有不同。
 *
 * 服务注册
 *  commons 模块只定义自动注册接口 {@link }、和一个模版抽象实现 {@link }，
 *  具体实现留给第三方（Eureka、Consul、Nacos）
 *
 * 服务发现
 *  commons 模块不光定义了发现客户端接口 {@link DiscoveryClient}、
 *  {@link ReactiveDiscoveryClient}，
 *  还在本包中提供默认简单实现 {@link SimpleDiscoveryClient}，
 *  以及处理外部第三方发现客户端的组合适配客户端
 *  {@link CompositeDiscoveryClient}、{@link ReactiveCompositeDiscoveryClient}
 *
 * 负载均衡
 *  commons 模块定义负载均衡客户端接口 {@link LoadBalancerClient}，
 *  负载均衡接口 {@link ReactiveLoadBalancer}，
 *  负载均衡交换过滤器 {@link LoadBalancedExchangeFilterFunction}
 *  与此同时，也将一部分负载均衡抽象分拆到 spring-cloud-loadbalancer 模块之中，
 *  如 {@link ReactorLoadBalancer}、{@link ReactorServiceInstanceLoadBalancer}
 *  且这个模块还有 Spring 对负载均衡器客户端的默认实现 {@link BlockingLoadBalancerClient}，
 *  以及两个不同负载均衡算法的负载均衡器 {@link RandomLoadBalancer}、{@link RoundRobinLoadBalancer},
 *  以及用来处理外部第三方负载均衡器客户端工厂 {@link LoadBalancerClientFactory}，
 *  它能为每个客户端名生成一个上下文，在上下文中加载通过
 *  {@link org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient}、
 *  {@link LoadBalancerClients} 两个注解的属性来指定自定义的负载均衡器的配置类
 *
 * 【说明】
 *  本示例只用来说明 spring-cloud-commons、spring-cloud-loadbalancer 两部分
 *  关于负载均衡的抽象部分的演示，至于 spring-cloud-loadbalancer 模块自身对负载均衡
 *  的实现及其提供的特有功能，请参考：loadbalancer-samples 模块
 *
 * 负载均衡 spring-cloud-commons 部分
 *
 * 1. @LoadBalanced 注解语义
 *  {@link LoadBalanced} 注解用来将 {@link RestTemplate}、{@link WebClient}
 *  类型的 bean 利用 {@link LoadBalancerClient} 而具备负载均衡的能力。
 *
 *  1.1 注解 RestTemplate
 *   原理：
 *    {@link LoadBalancerAutoConfiguration.LoadBalancerInterceptorConfig} 配置类
 *    定义拦截器 {@link LoadBalancerInterceptor}，并将其通过 {@link RestTemplateCustomizer}
 *    将容器中所有的 {@link RestTemplate} 添加该拦截器。具体收集 RestTemplate，并执行负载均衡拦截器定制器
 *    的时机为单例对象初始化完成之后，具体参考：
 *    {@link LoadBalancerAutoConfiguration#loadBalancedRestTemplateInitializerDeprecated(ObjectProvider)}
 *
 *  1.2 注解 WebClient.Builder
 *   原理：
 *    {@link LoadBalancerBeanPostProcessorAutoConfiguration} 自动装配类
 *    定义的 Bean 后置处理器 {@link LoadBalancerWebClientBuilderBeanPostProcessor}，
 *    将类型为 {@link WebClient.Builder} 类型的 Bean
 *    添加 {@link DeferringLoadBalancerExchangeFilterFunction} 类型的门面过滤器，
 *    它代理的具体过滤器为 {@link LoadBalancedExchangeFilterFunction}，这个过滤器在配置类
 *    {@link ReactorLoadBalancerClientAutoConfiguration} 中有两种具体实现的 Bean 定义：
 *    {@link ReactorLoadBalancerExchangeFilterFunction}
 *    {@link RetryableLoadBalancerExchangeFilterFunction} 具备 retry 特性
 *    具体根据属性配置：
 *      spring.cloud.loadbalancer.retry.enabled 是否开启，进行条件装配
 *
 *
 * 负载均衡 spring-cloud-loadbalancer 部分
 *
 *  该模块提供了对负载均衡的抽象与实现。针对负载均衡，它定义接口 {@link ReactiveLoadBalancer}
 *  以及实现类 {@link RandomLoadBalancer}、{@link RoundRobinLoadBalancer} 来实现负载均衡。
 *  而负载均衡中使用的服务实例列表则由接口 {@link ServiceInstanceListSupplier} 进行规范定义。
 *  目前，spring-cloud-loadbalancer 使用基于服务发现客户端形式的实现 {@link DiscoveryClientServiceInstanceListSupplier}。
 *
 * 1. 预加载 LoadBalancer 上下文
 *  Sprig Cloud LoadBalancer 为每一个服务创建独立的 Spring 子上下文，默认这些上下文的创建
 *  采用懒加载形式，即当某个服务被负载均衡使用时才创建其对应的子上下文，可以在 application.yaml
 *  中配置属性（参考本示例文件配置）：
 *      spring.cloud.loadbalancer.eager-load.clients=loadbalancer
 *  将指定的服务（本设置：loadbalancer）在 {@link ApplicationReadyEvent} 时创建子上下文。
 *
 *  原理：
 *   参考自动配置类（注意区别 commons 包下同名配置类 {@link LoadBalancerAutoConfiguration}）：
 *   {@link org.springframework.cloud.loadbalancer.config.LoadBalancerAutoConfiguration}
 *   定义的 Bean {@link LoadBalancerEagerContextInitializer}。
 *
 * 2. @LoadBalancerClients @LoadBalancerClient 语义 (负载均衡算法切换)
 *
 *  2.1 @LoadBalancerClients 和 @LoadBalancerClient 定义子上下文配置类的优先级
 *   通过以下注解的属性方法可以设置子上下的 Bean 定义配置类：
 *    LoadBalancerClients#defaultConfiguration
 *    LoadBalancerClient#configuration
 *
 *   这两个属性方法，分别用来设置所有子上下公共的默认配置及特定名称的子上下文独有的配置，
 *   其注册优先级为：
 *    先注册 LoadBalancerClient#configuration 配置类的 Bean
 *    后追加 LoadBalancerClients#defaultConfiguration 配置类的 Bean
 *    最后添加 {@link LoadBalancerClientConfiguration} 配置类的 Bean
 *      原理参考：
 *       {@link NamedContextFactory#registerBeans(String, GenericApplicationContext)}
 *   这样做的主要目的是：
 *    负载均衡器优先使用特定名称指定的配置类定义的，如果没定义，再使用 defaultConfiguration
 *    配置类定义的，如果该配置也没定义，最后兜底使用 LoadBalancerClientConfiguration 配置类
 *    定义的 {@link RoundRobinLoadBalancer}
 *
 *  2.2 根据 serviceId 名称生成相互隔离的子上下文
 *    之所以要生成子上下文，主要是为了针对不同的服务定义不同的负载均衡算法 Bean 等配置类，
 *    同容器是很难做到这样的效果。
 *    Spring 使用 {@link LoadBalancerClientFactory} 结合它的父类 {@link NamedContextFactory}
 *    来实现子上下文的隔离效果。
 *
 *  2.3 基于 {@link DiscoveryClientServiceInstanceListSupplier} 的各种包装类实现
 *    这些包装器实现通过对 {@link DiscoveryClientServiceInstanceListSupplier} 包装，实现包括缓存型、
 *    重试型、权重型等不同功能的负载均衡策略，由于此属于具体实现，将放到 loadbalancer-samples 模块介绍
 *
 * </pre>
 *
 * @author Reion
 * @date 2023-06-16
 **/
@Slf4j
@SpringBootApplication
public class LoadBalancerBootstrap {
    public static void main(String[] args) throws InterruptedException {
        ConfigurableApplicationContext context = SpringApplication.run(LoadBalancerBootstrap.class, args);
        Environment env = context.getEnvironment();

        // 睡眠一段时间，等客户端拉取 Nacos 上已注册服务列表信息
        Thread.sleep(2000);

        //1 @LoadBalanced 注解语义
        //1.1 注解 RestTemplate，由 loadBalancedRestTemplate() 方法定义
        RestTemplate restTemplate = (RestTemplate) context.getBean("loadBalancedRestTemplate");
        // 使用服务名 loadbalancer 访问服务
        log.info("@LoadBalanced RestTemplate Result: {}",
                restTemplate.getForObject("http://loadbalancer/server/address", String.class));
        //1.2 注解 WebClient.Builder，由 loadBalancedWebClientBuilder() 方法定义
        WebClient.Builder builder = (WebClient.Builder) context.getBean("loadBalancedWebClientBuilder");
        log.info("@LoadBalanced WebClient Result: {}",
                builder.build().get().uri("http://loadbalancer/server/address").retrieve().bodyToMono(String.class).block());

        //2 @LoadBalancerClient 注解语义（对不同服务设置不同负载均衡算法）
        // 由 嵌套配置类 InnerConfiguration 上的注解 @LoadBalancerClients 配置负载均衡算法
        LoadBalancerClientFactory clientFactory = context.getBean(LoadBalancerClientFactory.class);
        String serviceId = env.getProperty("spring.application.name");
        log.info("服务：{} 使用默认负载均衡器：{}", "foo", clientFactory.getInstance("foo").getClass().getSimpleName());
        log.info("服务：{} 使用指定负载均衡器：{}", serviceId, clientFactory.getInstance(serviceId).getClass().getSimpleName());
    }

    /**
     * 使用 @LoadBalanced 注解 RestTemplate
     */
    @Bean
    @LoadBalanced
    public RestTemplate loadBalancedRestTemplate() {
        return new RestTemplate();
    }

    /**
     * 使用 @LoadBalanced 注解 WebClient.Builder
     */
    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }

    /**
     * 此处的配置类单纯只用来设置服务 loadbalancer 使用 RandomLoadBalancer 负载均衡器
     *
     * 另：
     *   @LoadBalancerClient 注解可以放置在启动器 LoadBalancerBootstrap 上面
     *   为了更好的突出此注解的作用、对该注解的说明注释不与 LoadBalancerBootstrap 冲突
     *   才另外声明了一个嵌套内部配置类
     */
    @Configuration
    // 此注解支持多个 @LoadBalancerClient 组合，另外可以设置默认配置
    @LoadBalancerClients({
            // foo 没配置 configuration，使用当前容器中默认负载均衡器
            @org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient(name = "foo"),

            // loadbalancer 指定了 CustomLoadBalancerConfiguration 配置文件中的负载均衡器 RandomLoadBalancer
            @org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient(name = "loadbalancer",
                    configuration = CustomLoadBalancerConfiguration.class)
    })
    public static class InnerConfiguration {
    }
}
