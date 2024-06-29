package io.github.reionchan.circular;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Reion
 * @date 2024-06-30
 **/
@Slf4j
@Component
public class ServiceB {
    @Autowired
    private ServiceA serviceA;

    public void print() {
        log.info("{} - {}", ServiceB.class.getSimpleName(), this.getClass());
        log.info("{}.{} - {}", ServiceB.class.getSimpleName(), ServiceA.class.getSimpleName(), serviceA.getClass());
    }
}
