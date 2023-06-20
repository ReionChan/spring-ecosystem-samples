package outside.scan.config;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.core.RandomLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ReactorLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * 自定义负载均衡算法配置
 *
 * <pre>
 * 【注意】
 *      要使该负载均衡算法只被运用到指定的服务，请务必符合如下条件：
 *      1. 该配置类不要使用 @Configuration 等派生于 @Component 的组件注解 或 该类放置在组件扫描包范围之外
 *      2. 将该类设置在 @LoadBalancerClient 的 configuration 属性方法中
 * </pre>
 *
 * @author Reion
 * @date 2023-06-16
 **/
public class CustomLoadBalancerConfiguration {

    /**
     * 指定启用该配置的服务使用 RandomLoadBalancer 负载均衡器
     */
    @Bean
    ReactorLoadBalancer<ServiceInstance> randomLoadBalancer(Environment environment,
        LoadBalancerClientFactory loadBalancerClientFactory) {
        String name = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);
        return new RandomLoadBalancer(loadBalancerClientFactory
                .getLazyProvider(name, ServiceInstanceListSupplier.class), name);
    }
}
