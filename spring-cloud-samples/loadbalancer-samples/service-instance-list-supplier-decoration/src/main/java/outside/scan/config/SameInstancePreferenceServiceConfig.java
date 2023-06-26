package outside.scan.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 同实例优先服务列表提供配置
 *
 * @author Reion
 * @date 2023-06-22
 **/
@Slf4j
@Configuration
public class SameInstancePreferenceServiceConfig {
    @Bean
    public ServiceInstanceListSupplier sameInstancePreferenceServiceInstanceListSupplier(ConfigurableApplicationContext context) {
        log.info("=== SameInstancePreferenceServiceConfig 设置 sameInstancePreferenceServiceInstanceListSupplier ===");
        // 注意此处没有配置 .withCaching()
        return ServiceInstanceListSupplier.builder().withBlockingDiscoveryClient().withSameInstancePreference()
                .build(context);
    }
}
