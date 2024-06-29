package io.github.reionchan.lifecycle;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;

/**
 * <pre>
 * {@link org.springframework.security.config.annotation.configuration.AutowireBeanFactoryObjectPostProcessor#afterSingletonsInstantiated()}
 * </pre>
 * @author Reion
 **/
@Slf4j
@Component
public class Lifecycle_5_SmartInitializingSingleton implements SmartInitializingSingleton {
    /**
     * {@link org.springframework.security.config.annotation.configuration.AutowireBeanFactoryObjectPostProcessor#postProcess(Object)}
     * {@link org.springframework.security.config.annotation.configuration.AutowireBeanFactoryObjectPostProcessor#afterSingletonsInstantiated()}
     */
    @Override
    public void afterSingletonsInstantiated() {
        log.info("【第十】{} - {}", SmartInitializingSingleton.class.getSimpleName(), "所有单例 Bean 实例化完毕后");
    }
}
