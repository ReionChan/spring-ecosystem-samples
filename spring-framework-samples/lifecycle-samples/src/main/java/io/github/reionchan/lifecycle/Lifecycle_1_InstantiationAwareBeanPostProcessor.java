package io.github.reionchan.lifecycle;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.stereotype.Component;

/**
 * <pre>
 * {@link AbstractAutowireCapableBeanFactory#createBean(String, RootBeanDefinition, Object[])}
 * {@link AbstractAutowireCapableBeanFactory#resolveBeforeInstantiation}
 * {@link AbstractAutowireCapableBeanFactory#populateBean(String, RootBeanDefinition, BeanWrapper)}
 *
 * 代表类：
 *  {@link AbstractAutoProxyCreator}
 * </pre>
 * @author Reion
 **/
@Slf4j
@Component
public class Lifecycle_1_InstantiationAwareBeanPostProcessor implements InstantiationAwareBeanPostProcessor {
    /**
     * {@link AbstractAutowireCapableBeanFactory#resolveBeforeInstantiation}
     */
    @Override
    public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
        log.info("【第一】{} - {} Bean: {}", InstantiationAwareBeanPostProcessor.class.getSimpleName(), "实例化前", beanName);
        return InstantiationAwareBeanPostProcessor.super.postProcessBeforeInstantiation(beanClass, beanName);
    }

    /**
     * {@link AbstractAutowireCapableBeanFactory#resolveBeforeInstantiation}
     * {@link AbstractAutowireCapableBeanFactory#populateBean(String, RootBeanDefinition, BeanWrapper)}
     */
    @Override
    public boolean postProcessAfterInstantiation(Object bean, String beanName) throws BeansException {
        log.info("【第三】{} - {} Bean: {}", InstantiationAwareBeanPostProcessor.class.getSimpleName(), "实例化后", beanName);
        return InstantiationAwareBeanPostProcessor.super.postProcessAfterInstantiation(bean, beanName);
    }

    /**
     * 属性填充 populateBean
     * {@link AbstractAutowireCapableBeanFactory#populateBean(String, RootBeanDefinition, BeanWrapper)}
     */
    @Override
    public PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName) throws BeansException {
        log.info("【第四】{} - {} Bean: {}", InstantiationAwareBeanPostProcessor.class.getSimpleName(), "处理实例属性", beanName);
        return InstantiationAwareBeanPostProcessor.super.postProcessProperties(pvs, bean, beanName);
    }
}
