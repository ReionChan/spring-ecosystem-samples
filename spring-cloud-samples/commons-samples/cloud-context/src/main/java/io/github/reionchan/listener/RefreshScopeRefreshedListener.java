package io.github.reionchan.listener;

import io.github.reionchan.bean.RefreshScopeAnnotationBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.cloud.util.ProxyUtils;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * RefreshScope 刷新后事件监听器
 *
 * @author Reion
 * @date 2023-06-10
 **/
@Slf4j
@Component
public class RefreshScopeRefreshedListener implements ApplicationListener<RefreshScopeRefreshedEvent> {

    @Autowired
    private RefreshScopeAnnotationBean refreshScopeAnnotationBean;

    @Override
    public void onApplicationEvent(RefreshScopeRefreshedEvent event) {
        // @RefreshScope 注解的的类都是 AOP 代理类，故先获得其代理的原始类对象
        Object target = ProxyUtils.getTargetObject(refreshScopeAnnotationBean);

        log.info("=== 监听到 RefreshScopeRefreshedEvent 事件 ===\n\n@RefreshScope AOP 增强类 {}@{}\n 代理的原始对象 \nRefreshScopeAnnotationBean@{}\n 依赖 \nEnvProperties@{}\n\n",
                refreshScopeAnnotationBean.getClass(), System.identityHashCode(refreshScopeAnnotationBean),
                System.identityHashCode(target), System.identityHashCode(refreshScopeAnnotationBean.getEnv()));
    }
}
