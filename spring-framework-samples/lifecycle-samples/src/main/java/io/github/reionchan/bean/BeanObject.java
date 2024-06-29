package io.github.reionchan.bean;

import io.github.reionchan.lifecycle.Lifecycle_4_InitializingBean;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.*;
import org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.stereotype.Component;

/**
 * @author Reion
 * @date 2024-06-29
 **/
@Slf4j
@Component
public class BeanObject extends Lifecycle_4_InitializingBean implements BeanNameAware, BeanClassLoaderAware, BeanFactoryAware, DisposableBean {
    /**
     * {@link AbstractAutowireCapableBeanFactory#initializeBean(String, Object, RootBeanDefinition)}
     */
    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        log.info("【第六】{} - {} {}", BeanClassLoaderAware.class.getSimpleName(), BeanObject.class.getSimpleName(), "Bean 感知：ClassLoader");
    }

    /**
     * {@link AbstractAutowireCapableBeanFactory#initializeBean(String, Object, RootBeanDefinition)}
     */
    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        log.info("【第七】{} - {} {}", BeanFactoryAware.class.getSimpleName(), BeanObject.class.getSimpleName(), "Bean 感知：BeanFactory");
    }

    /**
     * {@link AbstractAutowireCapableBeanFactory#initializeBean(String, Object, RootBeanDefinition)}
     */
    @Override
    public void setBeanName(String name) {
        log.info("【第五】{} - {} {}", BeanNameAware.class.getSimpleName(), BeanObject.class.getSimpleName(), "Bean 感知：BeanName");
    }

    @PostConstruct
    public void postConstruct() {
        log.info("【第九】{} - {} {}", "@PostConstruct", "构造后执行", "BeanObject");
    }

    @PreDestroy
    public void preDestroy() {
        log.info("【第十一】{} - {} {}", "@PreDestroy", "销毁前执行", "BeanObject");
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
    }

    @Override
    public void destroy() throws Exception {
        log.info("【第十二】{} - {} {}", DisposableBean.class.getSimpleName(), "销毁方法", "BeanObject");
    }
}
