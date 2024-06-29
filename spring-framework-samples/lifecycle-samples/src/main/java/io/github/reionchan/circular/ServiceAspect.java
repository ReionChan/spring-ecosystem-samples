package io.github.reionchan.circular;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

/**
 * @author Reion
 * @date 2024-06-30
 **/
@Slf4j
@Aspect
@Component
public class ServiceAspect {

    @Before("execution(public void io.github.reionchan.circular.ServiceA.print())")
    public void before(JoinPoint joinPoint) {
        System.out.println("----- before ------");
    }
}
