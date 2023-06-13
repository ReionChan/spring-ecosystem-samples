package io.github.reionchan.bean;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 关联配置属性的 Bean
 *
 * <pre>
 * 当 {@link org.springframework.core.env.Environment} 发生变化时，
 * 产生 {@link org.springframework.cloud.context.environment.EnvironmentChangeEvent} 事件
 * 将被 {@link org.springframework.cloud.context.properties.ConfigurationPropertiesRebinder} 监听器监听，
 * 并激发 @ConfigurationProperties 注解的 Bean 的生命周期事件的执行（先销毁，后初始化）
 * </pre>
 *
 * @author Reion
 * @date 2023-06-10
 **/
@Slf4j
@Data
@Component
@ConfigurationProperties(prefix = "env")
public class EnvProperties implements InitializingBean, DisposableBean {

    private String p1;

    private String p2;

    @PostConstruct
    public void init() {
        log.info("{} 执行 @PostConstruct 方法", EnvProperties.class.getSimpleName());
    }

    @PreDestroy
    public void exit() {
        log.info("{} 执行 @PreDestroy 方法，p1={} p2={}", EnvProperties.class.getSimpleName(), p1, p2);
    }

    @Override
    public void destroy() throws Exception {
        log.info("{} 执行 destroy()", EnvProperties.class.getSimpleName());
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("{} 执行 afterPropertiesSet()，p1={} p2={}", EnvProperties.class.getSimpleName(), p1, p2);
    }
}
