package outside.scan.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 基于权重的服务列表提供配置
 *
 * @author Reion
 * @date 2023-06-22
 **/
@Slf4j
@Configuration
public class WeightedServiceConfig {
    @Bean
    public ServiceInstanceListSupplier weightedServiceInstanceListSupplier(ConfigurableApplicationContext context) {
        log.info("=== WeightedServiceConfig 设置 weightedServiceInstanceListSupplier ===");
        return ServiceInstanceListSupplier.builder().withBlockingDiscoveryClient().withWeighted().withCaching()
                .build(context);
    }
}
