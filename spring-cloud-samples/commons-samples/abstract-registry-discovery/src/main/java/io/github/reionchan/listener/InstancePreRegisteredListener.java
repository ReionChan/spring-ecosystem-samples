package io.github.reionchan.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.discovery.event.InstancePreRegisteredEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * 实例注册前事件监听器
 *
 * @author Reion
 * @date 2023-06-15
 **/
@Slf4j
@Component
public class InstancePreRegisteredListener implements ApplicationListener<InstancePreRegisteredEvent> {
    @Override
    public void onApplicationEvent(InstancePreRegisteredEvent event) {
        log.info("=== 监听到 InstancePreRegisteredEvent 注册前事件 ===\n待注册服务实例：{}", event.getRegistration().getUri().toString());
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
