package io.github.reionchan.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.bus.event.SentApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * 监听总线中除 Ack 以外的远程应用消息事件
 *
 * @author Reion
 * @date 2023-11-11
 **/
@Slf4j
@Component
public class SentApplicationEventListener implements ApplicationListener<SentApplicationEvent> {
    @Override
    public void onApplicationEvent(SentApplicationEvent event) {
        log.info("\n\n=== A remote event was found ===\nID:\t{}\nOrigin:\t{}\nDestination:\t{}\nType:\t{}\n",
                event.getId(), event.getOriginService(), event.getDestinationService(), event.getType().getSimpleName());
    }
}
