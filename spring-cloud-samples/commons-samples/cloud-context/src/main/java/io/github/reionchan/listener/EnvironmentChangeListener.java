package io.github.reionchan.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 环境变量变更监听器
 *
 * @author Reion
 * @date 2023-06-10
 **/
@Slf4j
@Component
public class EnvironmentChangeListener implements ApplicationListener<EnvironmentChangeEvent> {

    @Override
    public void onApplicationEvent(EnvironmentChangeEvent event) {
        StringBuffer strBuffer = new StringBuffer("=== 监听到 EnvironmentChangeEvent 事件 ===\n");
        Set<String> keys =  event.getKeys();
        keys.stream().map(key -> key.concat("\n")).forEach(strBuffer::append);
        log.info(strBuffer.toString());
    }
}
