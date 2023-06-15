package io.github.reionchan.listener;

import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.discovery.event.InstanceRegisteredEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;

/**
 * 实例已注册事件监听器
 *
 * @author Reion
 * @date 2023-06-15
 **/
@Slf4j
@Data
@Component("latchInstanceRegisteredListener")
public class InstanceRegisteredListener implements ApplicationListener<InstanceRegisteredEvent> {

    @Override
    public void onApplicationEvent(InstanceRegisteredEvent event) {
        log.info("=== 监听到 InstanceRegisteredEvent 注册完成事件 ===\n\n{}\n", event.getConfig());
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
