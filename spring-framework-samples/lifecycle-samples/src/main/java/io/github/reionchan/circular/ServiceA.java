package io.github.reionchan.circular;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.framework.autoproxy.AutoProxyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Reion
 * @date 2024-06-30
 **/
@Slf4j
@Component
public class ServiceA {

    @Autowired
    private ServiceB serviceB;

    public void print() {
        log.info("{} - {}", ServiceA.class.getSimpleName(), this.getClass());
        log.info("{}.{} - {}", ServiceA.class.getSimpleName(), ServiceB.class.getSimpleName(), serviceB.getClass());
        serviceB.print();
    }
}
