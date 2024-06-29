package io.github.reionchan.lifecycle;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.stereotype.Component;

/**
 * <pre>
 * {@link AbstractAutowireCapableBeanFactory#invokeInitMethods(String, Object, RootBeanDefinition)}
 * </pre>
 * @author Reion
 **/
@Slf4j
@Component
public class Lifecycle_4_InitializingBean implements InitializingBean {
    /**
     * {@link AbstractAutowireCapableBeanFactory#initializeBean(String, Object, RootBeanDefinition)}
     *      {@link AbstractAutowireCapableBeanFactory#invokeInitMethods(String, Object, RootBeanDefinition)}
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("【第九】{} - {}", InitializingBean.class.getSimpleName(), "Bean 属性设置之后");
    }
}
