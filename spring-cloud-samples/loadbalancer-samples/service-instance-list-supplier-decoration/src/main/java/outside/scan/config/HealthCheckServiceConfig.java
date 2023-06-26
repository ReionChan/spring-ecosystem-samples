package outside.scan.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 基于健康检测的服务列表提供配置
 *
 * @author Reion
 * @date 2023-06-22
 **/
@Slf4j
@Configuration
public class HealthCheckServiceConfig {
    @Bean
    public ServiceInstanceListSupplier healthCheckServiceInstanceListSupplier(ConfigurableApplicationContext context) {
        log.info("=== HealthCheckServiceConfig 设置 healthCheckServiceInstanceListSupplier ===");
        // 注意此处没有配置 .withCaching()
        return ServiceInstanceListSupplier.builder().withBlockingDiscoveryClient().withBlockingHealthChecks()
                .build(context);
    }
}
