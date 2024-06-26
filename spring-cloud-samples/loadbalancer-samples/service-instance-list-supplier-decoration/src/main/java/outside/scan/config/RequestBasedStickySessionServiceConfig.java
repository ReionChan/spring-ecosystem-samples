package outside.scan.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 基于请求 cookie 的服务列表提供配置
 *
 * @author Reion
 * @date 2023-06-22
 **/
@Slf4j
@Configuration
public class RequestBasedStickySessionServiceConfig {

    @Bean
    public ServiceInstanceListSupplier requestBasedStickyServiceInstanceListSupplier(ConfigurableApplicationContext context) {
        log.info("=== RequestBasedStickyServiceConfig 设置 requestBasedStickyServiceInstanceListSupplier ===");
        // withCaching() makes sticky session not working. see https://github.com/spring-cloud/spring-cloud-commons/issues/1249
        // 4.0.4 版本已修复，将 withCaching 与 withRequestBasedStickySession 互换位置
        return ServiceInstanceListSupplier.builder().withBlockingDiscoveryClient().withCaching()
                .withRequestBasedStickySession().build(context);
    }
}
