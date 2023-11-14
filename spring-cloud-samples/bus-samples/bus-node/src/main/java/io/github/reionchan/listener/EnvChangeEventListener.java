package io.github.reionchan.listener;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 环境变量改变监听器
 *
 * @author Reion
 * @date 2023-11-11
 **/
@Slf4j
@Component
public class EnvChangeEventListener implements ApplicationListener<EnvironmentChangeEvent> {

    @Resource
    private ConfigurableEnvironment env;

    @Override
    public void onApplicationEvent(EnvironmentChangeEvent event) {
        try {
            Set<String> changeKeys = event.getKeys();
            if (changeKeys.isEmpty()) {
                log.info("\n\nNo change keys\n");
                return;
            }
            StringBuilder strBuilder = new StringBuilder("\n\n=== Changed Keys===\n");
            for (String key : changeKeys) {
                strBuilder.append(key).append(" --> ").append(env.getProperty(key)).append("\n");
            }
            log.info(strBuilder.toString());
        } catch (Exception e) {
            log.error("Error!", e);
        }
    }
}
