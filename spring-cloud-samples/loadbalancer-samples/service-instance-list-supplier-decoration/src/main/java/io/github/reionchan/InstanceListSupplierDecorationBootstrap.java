package io.github.reionchan;

import io.github.reionchan.lifecycle.CustomLoadBalancerLifecycle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.CacheManager;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.*;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.cloud.loadbalancer.cache.CaffeineBasedLoadBalancerCacheManager;
import org.springframework.cloud.loadbalancer.cache.DefaultLoadBalancerCacheManager;
import org.springframework.cloud.loadbalancer.core.*;
import org.springframework.cloud.loadbalancer.stats.MicrometerStatsLoadBalancerLifecycle;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import outside.scan.config.*;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * 服务实例列表提供器装饰实现启动器
 *
 * <pre>
 * 此模块用来验证 Spring Cloud LoadBalancer 的各种 {@link ServiceInstanceListSupplier} 实现，
 * 其中 {@link DiscoveryClientServiceInstanceListSupplier} 为基本实现，它可以被其他各种实现装饰，
 * 从而达到不同的负载均衡效果。
 *
 * 1. 本模块运行需要依赖 Nacos 以及 3.1.3 abstract-loadbalancer 服务，
 *    其中 abstract-loadbalancer 作为负载均衡目标服务，启动不同端口至少两个该服务实例。
 * 2. {@link InnerClass} 上的 @LoadBalancerClient 注解，
 *    用来对 abstract-loadbalancer 实例配置不同的 {@link ServiceInstanceListSupplier} 实现类装饰
 *    例如：
 *      要测试 {@link ZonePreferenceServiceInstanceListSupplier}，请将以下注解的注释放开
 *       configuration = ZoneBasedServiceConfig.class
 *      将其余的注解暂时注释，以此类推
 *
 *    针对每种装饰实现，可能要对 abstract-loadbalancer 服务及本服务的
 *    application.yaml 文件进行配置调整，都在测试方法上有注释说明，参考说明进行操作即可。
 *
 * 3. 此外，负载均衡生命周期接口 {@link LoadBalancerLifecycle} 自定义实现 {@link CustomLoadBalancerLifecycle}
 *    它可以对负载均衡各个生命周期节点定义回调操作。
 *
 * 4. 同时，框架自带生命周期接口实现类 {@link MicrometerStatsLoadBalancerLifecycle} 用来生成
 *    负载均衡的统计信息，通过在 application.yaml 文件设置属性
 *      spring.cloud.loadbalancer.stats.micrometer.enabled=true
 *    来激活统计，并可以通过 actuator 的 metrics 端点访问如下统计信息：
 *    loadbalancer.requests.active
 *    loadbalancer.requests.success
 *    loadbalancer.requests.failed
 *    loadbalancer.requests.discard
 *
 * </pre>
 *
 * @author Reion
 * @date 2023-06-21
 **/
@Slf4j
@SpringBootApplication
public class InstanceListSupplierDecorationBootstrap {

    @Configuration
    @LoadBalancerClient(name = "loadbalancer", configuration = WeightedServiceConfig.class)
//    @LoadBalancerClient(name = "loadbalancer", configuration = ZoneBasedServiceConfig.class)
//    @LoadBalancerClient(name = "loadbalancer", configuration = HealthCheckServiceConfig.class)
//    @LoadBalancerClient(name = "loadbalancer", configuration = SameInstancePreferenceServiceConfig.class)
//    @LoadBalancerClient(name = "loadbalancer", configuration = RequestBasedStickySessionServiceConfig.class)
//    @LoadBalancerClient(name = "loadbalancer", configuration = HintBasedServiceConfig.class)
    static class InnerClass {
    }

    /**
     * 使用 @LoadBalanced 注解 RestTemplate
     */
    @Bean
    @Primary
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    @LoadBalanced
    @Qualifier("lb")
    public RestTemplate loadBalancedRestTemplate() {
        return new RestTemplate();
    }

    public static void main(String[] args) throws InterruptedException, URISyntaxException, IOException {
        ConfigurableApplicationContext context = SpringApplication.run(InstanceListSupplierDecorationBootstrap.class, args);
        Environment env = context.getEnvironment();
        String serviceId = env.getProperty("spring.application.name");

        // 由于 abstract-loadbalancer 模块可知，每个服务有其独立的子上下文，包含在此上下文之中
        // 在 application.yaml 中已设置预加载本模块服务的子上下文，故 ServiceInstanceListSupplier 的实现类可在此工厂获取
        LoadBalancerClientFactory loadBalancerClientFactory = context.getBean(LoadBalancerClientFactory.class);
        Thread.sleep(2000);

        /**
         * 1. 默认设置时 ServiceInstanceListSupplier 默认装饰类结构信息
         *
         * === ServiceInstanceListSupplier Info ===
         * 	{@link CachingServiceInstanceListSupplier}
         * 		{@link DiscoveryClientServiceInstanceListSupplier}
         *
         * 默认情况：
         *  {@link DiscoveryClientServiceInstanceListSupplier}
         *  被 {@link CachingServiceInstanceListSupplier} 装饰，提供缓存服务列表功能，就不必每次都发起远程调用获得服务列表。
         *  缓存底层使用的是 {@link CacheManager}，默认实现 {@link DefaultLoadBalancerCacheManager}
         *  生产环境推荐使用 {@link CaffeineBasedLoadBalancerCacheManager}，需要引入依赖 caffeine 来激活此缓存管理器
         */
        ServiceInstanceListSupplier supplier = loadBalancerClientFactory.getInstance(serviceId, ServiceInstanceListSupplier.class);
        inspectSupplier(supplier);
        supplier.get().flatMap(Flux::fromIterable).map(serviceInstance -> serviceInstance.getUri().toString()).subscribe(uri -> {
            log.info(uri);
        });

        /**
         * 2. 以 3.1.3 项目（即：服务名 loadbalancer）为目标服务，用本类中 @LoadBalancerClients 指定其子上下文配置文件
         *
         * 2.1 当使用 {@link WeightedServiceConfig}，设置基于权重配置的负载均衡，分别在目标项目启动两个实例
         *     在其 application.yaml 文件的 spring.cloud.nacos.discovery.meta.weigh 设置权重分别为 2 和 8
         *     观察日志输出：
         *
         *     === ServiceInstanceListSupplier Info ===
         * 	      {@link CachingServiceInstanceListSupplier}
         * 		      {@link WeightedServiceInstanceListSupplier}
         * 				{@link DiscoveryClientServiceInstanceListSupplier}
         *
         *    相较默认设置，中间增加 WeightedServiceInstanceListSupplier 装饰，它提供了按照权重大小分配多个实例被选择的几率，
         *    下面测试尝试在两个 loadbalancer 实例，选择 10 次，可以看出权重为 8 的服务器被选中次数明显更多，几率在 80%
         *
         * 2.2 使用 {@link ZoneBasedServiceConfig}，设置基于 Zone 配置的负载均衡，分别在目标项目启动两个实例
         *     在其 application.yaml 文件的 spring.cloud.nacos.discovery.meta.zone 设置区域分别为 foo-zone 和 bar-zone
         *     在本应用的 application.yaml 文件的 spring.cloud.loadbalancer.zone 设置仅使用 bar-zone 标识的服务器实例
         *     观察日志输出：
         *
         *     === ServiceInstanceListSupplier Info ===
         * 	     {@link CachingServiceInstanceListSupplier}
         * 		     {@link ZonePreferenceServiceInstanceListSupplier}
         * 				{@link DiscoveryClientServiceInstanceListSupplier}
         *
         * 	   相较默认设置，中间增加 ZonePreferenceServiceInstanceListSupplier 装饰，它提供根据 zone 设置过滤服务器实例
         * 	   下面测试尝试在两个 loadbalancer 实例，选择 10 次，可以看出仅使用标识为 bar-zone 的服务器实例
         *
         * 2.3 使用 {@link HealthCheckServiceConfig}，设置基于 HealthCheck 配置的负载均衡，分别在目标项目启动两个实例
         *     在其 application.yaml 文件 management.endpoints.web.exposure.include 增加 health 端点，并且引入 spring boot actuator
         *     在本应用的 application.yaml 文件的 spring.cloud.loadbalancer.health-check.* 配置相关参数
         *     观察日志输出：
         *
         *     === ServiceInstanceListSupplier Info ===
         * 	    {@link HealthCheckServiceInstanceListSupplier}
         * 		   {@link DiscoveryClientServiceInstanceListSupplier}
         *
         *     相较默认设置，移去了缓存，增加 HealthCheckServiceInstanceListSupplier 装饰，它提供根据健康情况刷新服务器实例
         *     值得注意的是存在服务自动注册与发现的情境中没有必要设置此装饰器，因为自动注册与发现已然会根据服务心跳动态刷新服务。
         *
         * 2.4 使用 {@link SameInstancePreferenceServiceConfig}，设置相同服务实例优先的负载均衡，分别在目标项目启动两个实例
         *     观察日志输出：
         *
         *     === ServiceInstanceListSupplier Info ===
         * 	    {@link SameInstancePreferenceServiceInstanceListSupplier}
         * 		    {@link DiscoveryClientServiceInstanceListSupplier}
         *
         * 	   相较默认设置，移去了缓存，增加 SameInstancePreferenceServiceInstanceListSupplier 装饰，它提供保存分配的服务实例尽量一致的特性
         * 	   初次调用后，它的接口回调 {@link SelectedInstanceCallback} 被负载均衡器调用，设置本次所选的服务实例，方便下次调用时直接复用。
         * 	   如果当前复用的服务器不可用时，会返回所有可用服务列表，然后再保存新选择的服务器实例，进行复用。
         *
         */
        supplier = loadBalancerClientFactory.getInstance("loadbalancer", ServiceInstanceListSupplier.class);
        inspectSupplier(supplier);
        org.springframework.cloud.client.loadbalancer.LoadBalancerClient client = context.getBean(org.springframework.cloud.client.loadbalancer.LoadBalancerClient.class);
        // RequestBasedStickySessionServiceInstanceListSupplier 采用 RestTemplate 形式测试 （因为需要根据 request 中的 cookie 来进行负载均衡）
        if (!(supplier instanceof RequestBasedStickySessionServiceInstanceListSupplier
                || supplier instanceof HintBasedServiceInstanceListSupplier)) {
            ServiceInstance instance = null;
            for (int i=0; i<10; i++) {
                Thread.sleep(1000);
                instance = client.choose("loadbalancer");
                if (instance != null) {
                    log.info("{}", instance.getUri().toString());
                } else {
                    log.info("--- No Service Instance for loadbalancer ---");
                }
            }
        }

        /**
         * 2.5 使用 {@link RequestBasedStickySessionServiceConfig}，设置相同服务实例优先的负载均衡，分别在目标项目启动两个实例
         *     在其 application.yaml 文件 spring.cloud.nacos.discovery.ephemeral=false
         *     使 NacosDiscoveryClient 发现的服务实例 {@link ServiceInstance} 都能生成 instanceId 属性
         *     在本应用的 application.yaml 文件的 spring.cloud.loadbalancer.sticky-session.* 配置相关参数
         *     下面使用支持负载均衡的 RestTemplate 模拟发送包含指定 instanceId 的 cookie 请求，观察日志输出：
         *
         *     === ServiceInstanceListSupplier Info ===
         * 	    {@link RequestBasedStickySessionServiceInstanceListSupplier}
         * 		    {@link DiscoveryClientServiceInstanceListSupplier}
         *
         *     相较默认设置，移去了缓存，增加 RequestBasedStickySessionServiceInstanceListSupplier 装饰，
         *     它提供根据 cookie 中选定的服务实例进行请求处理，值的注意，如果选的的服务实例不可用，将会选定其它可用的服务实例处理本次请求
         *       spring.cloud.loadbalancer.sticky-session.add-service-instance-cookie=true
         *     上面属性设置为 true 时，请求在交给新选定的服务实例处理前，会将新实例的 instanceId 追加到原来的 cookie 中，
         *     处理逻辑参见：{@link LoadBalancerServiceInstanceCookieTransformer}
         *     观察日志打印，可以看出 5 次请求都固定使用端口为 8088 的 loadbalancer 服务
         *
         * 2.6 使用 {@link HintBasedServiceConfig}，设置相同服务实例优先的负载均衡，分别在目标项目启动两个实例
         *     在其 application.yaml 文件的 spring.cloud.nacos.discovery.meta.hint 设置区域分别为 foo-hint 和 bar-hint
         *     在本应用的 application.yaml 文件的
         *       spring.cloud.loadbalancer.hint.loadbalancer.hint=foo-hint
         *     即：指定 serviceId 为 loadbalancer 的 hint 使用为 foo-hint 的实例
         *     同时，在下方请求的头中增加名称为 X-SC-LB-Hint 值为 bar-hint
         *     分别执行两次本应用
         *        第一次：请求头增加 X-SC-LB-Hint=bar-hint
         *        第二次：注释掉 X-SC-LB-Hint=bar-hint
         *     观察两次的日志输出：
         *
         *     === ServiceInstanceListSupplier Info ===
         * 	     {@link HintBasedServiceInstanceListSupplier}
         * 		    {@link DiscoveryClientServiceInstanceListSupplier}
         *
         * 	   相较默认设置，移去了缓存，增加 HintBasedServiceInstanceListSupplier 装饰，它提供根据 hint 设置过滤服务器实例
         * 	   首先，它优先从请求头中的 X-SC-LB-Hint 获取 hint 值，当做过滤条件；
         * 	   如果不存在，它再从 application.yaml 中的 spring.cloud.loadbalancer.hint.loadbalancer.hint=foo-hint 获取 hint 值
         * 	   如果还是不存在，它再设置默认的 hint 值 default
         *
         */
        RestTemplate restTemplate = context.getBean("loadBalancedRestTemplate", RestTemplate.class);
        if (supplier instanceof RequestBasedStickySessionServiceInstanceListSupplier
                || supplier instanceof HintBasedServiceInstanceListSupplier) {
            for (int i=0; i<3; i++) {
                HttpHeaders headers = new HttpHeaders();
                String info = "";
                if (supplier instanceof RequestBasedStickySessionServiceInstanceListSupplier) {
                    List<String> cookies = List.of("sc-lb-instance-id=192.168.1.102#8088#DEFAULT#DEFAULT_GROUP@@loadbalancer");
                    headers.put(HttpHeaders.COOKIE, cookies);
                    info = "Cookie 粘性选择端口为 8088 的服务实例，";
                }
                if (supplier instanceof HintBasedServiceInstanceListSupplier) {
                    headers.add("X-SC-LB-Hint", "bar-hint");
                    info = "Header 选择 Hint 为 bar-hint 的服务实例，";
                }

                RequestEntity<Void> requestEntity = RequestEntity.get(new URI("http://loadbalancer/server/address")).headers(headers).build();
                ResponseEntity<String> responseEntity = restTemplate.exchange(requestEntity, String.class);
                log.info("{} 返回结果: {}", info, responseEntity.getBody());
            }
        }
    }

    /**
     * 打印 {@link ServiceInstanceListSupplier} 类的包装关系信息
     */
    public static void inspectSupplier(ServiceInstanceListSupplier supplier) {
        StringBuffer strBuf = new StringBuffer("\n=== ServiceInstanceListSupplier Info ===\n");
        String tab = "\t";
        while (supplier instanceof DelegatingServiceInstanceListSupplier delegate) {
            strBuf.append(tab + delegate.getClass().getSimpleName() + "\n");
            supplier = delegate.getDelegate();
            tab = tab.concat(tab);
        }
        strBuf.append(tab + supplier.getClass().getSimpleName() + "\n");
        log.info(strBuf.toString());
    }
}
