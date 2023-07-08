package io.github.reionchan;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.bootstrap.BootstrapConfiguration;
import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.cloud.config.client.*;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.web.client.RestTemplate;

/**
 * 利用 Bootstrap 加载外部配置文件的配置客户端启动器
 *
 * <pre>
 * 加载外部化配置文件，Spring Boot 2.4.0 之前，一直采用的是 Spring Cloud Context
 * 下的 bootstrap 引导上下文机制，通过该引导上下文去加载外部化配置文件，然后再实例化
 * 子应用上下文，具体可以参考 3.1.1 Cloud Application Context 的描述，至于
 * Spring Boot 2.4.0 之后采取的 <a href="https://docs.spring.io/spring-cloud-config/docs/current/reference/html/#config-data-import">Config Data Import</a> 将在下一个示例说明。
 *
 * Bootstrap 上下文机制使用 Spring 工厂机制装载启动配置类 {@link BootstrapConfiguration}，
 * 而 spring-cloud-config-client 配置客户端包中，此属性配置了两个配置类：
 *  {@link ConfigServiceBootstrapConfiguration}
 *  {@link DiscoveryClientConfigServiceBootstrapConfiguration}
 * 前者主要负责 {@link ConfigClientProperties} 配置属性类的 Bean 定义，
 * 后者负责服务发现激活时，采用服务发现机制获得配置服务器实例，并构造配置请求 URL。
 * 本示例仅演示前者直接通过 spring.cloud.config.uri 直接指定配置服务器地址的形式。
 *
 * {@link ConfigServiceBootstrapConfiguration} 配置类装配两个关键 Bean:
 *  {@link ConfigClientProperties}
 *  {@link ConfigServicePropertySourceLocator}
 * 前者主要是收集 bootstrap.yaml 启动配置中有关配置服务器相关设置
 * 后者实现接口 {@link PropertySourceLocator}，该接口在 3.1.1 中有自定义实现样例及原理说明，
 * 本示例的实现类是用来从配置服务器获得 {@link PropertySource}，而它与配置服务器的
 * Http 通信使用 {@link ConfigClientRequestTemplateFactory} 工厂构建的 {@link RestTemplate}，
 * 具体逻辑参考：{@link ConfigServicePropertySourceLocator#locate(Environment)}
 *
 * </pre>
 *
 * @author Reion
 * @date 2023-07-08
 **/
@Slf4j
@SpringBootApplication
public class ConfigClientDoubleBootstrap {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(ConfigClientDoubleBootstrap.class, args);
        Environment env = context.getEnvironment();
        // 如果获得的属性值为空，请确保 bootstrap.yaml 的属性 spring.cloud.config.profile 的值
        // 包含在配置服务的 spring.profiles.active 属性配置值之中
        log.info("来自配置服务器的属性 demo.encrypt-key = {}", env.getProperty("demo.encrypt-key"));
    }
}
