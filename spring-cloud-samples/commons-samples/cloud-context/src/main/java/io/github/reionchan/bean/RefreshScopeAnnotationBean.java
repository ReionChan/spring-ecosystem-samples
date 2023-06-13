package io.github.reionchan.bean;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * 被 @RefreshScope 注解的 Bean
 *
 * @author Reion
 * @date 2023-06-10
 **/
@Data
@Slf4j
@Component
@RefreshScope
public class RefreshScopeAnnotationBean {

    @Autowired
    private EnvProperties env;

    // @formatter: off
    @Autowired
    public RefreshScopeAnnotationBean(EnvProperties env) {
        log.info("=== 调用 RefreshScopeAnnotationBean() 构造函数 ===\nRefreshScopeAnnotationBean@{} \n 依赖\nEnvProperties@{} \n",
                System.identityHashCode(this), System.identityHashCode(env));
        this.env = env;
    }
    // @formatter: on
}
