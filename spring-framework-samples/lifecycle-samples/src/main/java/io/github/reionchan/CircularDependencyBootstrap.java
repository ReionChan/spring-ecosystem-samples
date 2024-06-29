package io.github.reionchan;

import io.github.reionchan.circular.ServiceA;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator;
import org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Component;

/**
 * @author Reion
 **/
@Slf4j
@Component
@ComponentScan(basePackageClasses = ServiceA.class)
@EnableAspectJAutoProxy
public class CircularDependencyBootstrap {

    /**
     * {@link AbstractAutowireCapableBeanFactory#getEarlyBeanReference(String, RootBeanDefinition, Object)}
     *      {@link AbstractAutoProxyCreator#getEarlyBeanReference(Object, String)}
     */
    public static void main(String[] args) {
        // 注册当前引导类作为 Configuration Class
        ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(CircularDependencyBootstrap.class);
        // 获取 Bean
        ServiceA serviceA = (ServiceA) context.getBean("serviceA");
        serviceA.print();
        context.close();
    }
}
