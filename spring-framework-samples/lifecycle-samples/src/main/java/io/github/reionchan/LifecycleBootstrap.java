package io.github.reionchan;

import io.github.reionchan.bean.BeanObject;
import io.github.reionchan.lifecycle.Lifecycle_4_InitializingBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.*;
import org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
import org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanFactory;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * @author Reion
 * @date 2024-06-29
 **/
@Slf4j@Component
@ComponentScan(basePackageClasses = {BeanObject.class, Lifecycle_4_InitializingBean.class})
public class LifecycleBootstrap {
    /**
     * <pre>
     *  ### 实例前
     * 1. {@link InstantiationAwareBeanPostProcessor#postProcessBeforeInstantiation}
     * 2. {@link AbstractAutowireCapableBeanFactory#obtainFromSupplier}
     * 3. {@link AbstractAutowireCapableBeanFactory#instantiateUsingFactoryMethod}
     *
     * ### 实例化
     * 1. {@link AbstractBeanFactory#registerCustomEditors}
     * 2. {@link MergedBeanDefinitionPostProcessor#postProcessMergedBeanDefinition}
     *
     * ### 属性填充
     * 1. {@link InstantiationAwareBeanPostProcessor#postProcessAfterInstantiation}
     * 2. {@link InstantiationAwareBeanPostProcessor#postProcessPropertyValues}
     * 3. {@link SmartInstantiationAwareBeanPostProcessor#getEarlyBeanReference} （循环依赖，此处提前判断是否生成代理）
     *
     * ### 初始化前
     *
     * 1. {@link BeanNameAware#setBeanName}
     * 2. {@link BeanClassLoaderAware#setBeanClassLoader}
     * 3. {@link InitDestroyAnnotationBeanPostProcessor#postProcessBeforeInitialization} （@PostConstruct）
     * 4. {@link BeanPostProcessor#postProcessBeforeInitialization}
     *
     *
     * ### 初始化
     * 1. {@link InitializingBean#afterPropertiesSet}
     * 2. {@link Method#invoke} （指定 initMethod 方法）
     *
     * ### 初始化后
     * 1. {@link BeanPostProcessor#postProcessAfterInitialization} （未出现循环依赖，初始化完成后判断是否生成）
     *
     * ### 所有单例 Bean 初始化后
     * 1. {@link SmartInitializingSingleton#afterSingletonsInstantiated} （所有单例 Bean 初始化后，此处不会带来 Bean 过早初始化问题）
     *
     * ### 销毁前
     * 1. {@link InitDestroyAnnotationBeanPostProcessor#postProcessBeforeDestruction} （@PreDestroy）
     * 2. {@link DestructionAwareBeanPostProcessor#postProcessBeforeDestruction}
     *
     * ### 销毁
     * 1. {@link DisposableBean#destroy}
     * 2. {@link Method#invoke} （指定 destroyMethod 方法）
     * </pre>
     */
    public static void main(String[] args) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(LifecycleBootstrap.class);
        BeanObject bean = context.getBean(BeanObject.class);
        log.info("得到最终 Bean：{}", bean);
        context.close();
    }
}
