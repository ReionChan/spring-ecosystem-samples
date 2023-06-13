package bootstrap.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 启动器上下文中的配置类
 *
 * 验证 bootstrap 上下文通过 Spring Factories 工厂机制加载 BootstrapConfiguration 类型的配置 Bean
 *
 * 注意：
 * 此配置类所在的包，不在 {@link io.github.reionchan.EnabledBootstrapContextBootstrap} 启动器的扫描路径
 *
 * @author Reion
 * @date 2023-06-10
 **/
@Configuration
public class BootstrapContextConfiguration {

    @Bean
    public Object beanLoadBySpringFactoryInBootstrap() {
        return new Object();
    }
}
