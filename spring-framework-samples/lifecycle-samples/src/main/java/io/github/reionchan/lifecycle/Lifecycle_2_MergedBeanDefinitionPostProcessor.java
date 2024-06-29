package io.github.reionchan.lifecycle;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.stereotype.Component;

/**
 * <pre>
 * {@link AbstractAutowireCapableBeanFactory#applyMergedBeanDefinitionPostProcessors(RootBeanDefinition, Class, String)}
 * </pre>
 * @author Reion
 **/
@Slf4j
@Component
public class Lifecycle_2_MergedBeanDefinitionPostProcessor implements MergedBeanDefinitionPostProcessor {
    @Override
    public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
        log.info("【第二】{} - {} Bean: {}", MergedBeanDefinitionPostProcessor.class.getSimpleName(), "处理合并 BeanDefinition", beanName);
    }

    @Override
    public void resetBeanDefinition(String beanName) {
        log.info("{} - {} Bean: {}", MergedBeanDefinitionPostProcessor.class.getSimpleName(), "重置 BeanDefinition", beanName);
        MergedBeanDefinitionPostProcessor.super.resetBeanDefinition(beanName);
    }
}
