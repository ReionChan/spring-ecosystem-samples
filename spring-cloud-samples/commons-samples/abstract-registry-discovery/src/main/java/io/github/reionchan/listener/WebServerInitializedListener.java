package io.github.reionchan.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * Web 服务器初始化后事件监听器
 *
 * @author Reion
 * @date 2023-06-15
 **/
@Slf4j
@Component
public class WebServerInitializedListener implements ApplicationListener<WebServerInitializedEvent> {
    @Override
    public void onApplicationEvent(WebServerInitializedEvent event) {
        log.info("=== 监听到 WebServerInitializedEvent 事件 ===\n服务器实现类：{}\n服务器端口：{}",
                event.getWebServer().getClass().getSimpleName(), event.getWebServer().getPort());
    }
}
