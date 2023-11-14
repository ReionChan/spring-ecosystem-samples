package io.github.reionchan.listener;

import io.github.reionchan.event.NotificationRemoteApplicationEvent;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.bus.PathServiceMatcher;
import org.springframework.cloud.bus.event.AckRemoteApplicationEvent;
import org.springframework.cloud.bus.event.EnvironmentChangeRemoteApplicationEvent;
import org.springframework.cloud.bus.event.RefreshRemoteApplicationEvent;
import org.springframework.cloud.bus.event.RemoteApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 总线中发送及接收的事件记录器
 *
 * @author Reion
 * @date 2023-11-11
 **/
@Slf4j
@Component
public class RemoteApplicationEventRecoder implements ApplicationListener<RemoteApplicationEvent> {

    @Resource
    private PathServiceMatcher pathServiceMatcher;
    @Override
    public void onApplicationEvent(RemoteApplicationEvent event) {
        boolean isFromSelf = pathServiceMatcher.isFromSelf(event);

        StringBuilder strBid = new StringBuilder(isFromSelf ? "\n\n=== Sent To Bus ===\n" : "\n\n=== Received From Bus ===\n");
        strBid.append("ID: ").append(event.getId()).append("\n");
        strBid.append("From: ").append(event.getOriginService()).append("\n");
        strBid.append("To: ").append(event.getDestinationService()).append("\n");
        strBid.append("Type: ").append(event.getClass().getSimpleName()).append("\n");
        if (event instanceof AckRemoteApplicationEvent ackEvent) {
            strBid.append("ACK for EventID: ").append(ackEvent.getAckId()).append("\n");
            strBid.append("ACK for EventType: ").append(ackEvent.getEvent().getSimpleName()).append("\n");
            strBid.append("ACK for EventDestinationAddress: ").append(ackEvent.getAckDestinationService()).append("\n");
        } else if (event instanceof RefreshRemoteApplicationEvent refreshEvent) {

        } else if (event instanceof EnvironmentChangeRemoteApplicationEvent envChangeEvent) {
            Map<String, String> values = envChangeEvent.getValues();
            strBid.append("--- properties changes ---").append("\n");
            for (Map.Entry<String, String> entry : values.entrySet()) {
                strBid.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
        } else if (event instanceof NotificationRemoteApplicationEvent notificationEvent) {
            strBid.append("Notification ID: ").append(notificationEvent.getNotification().getId()).append("\n");
            strBid.append("Notification Timestamp: ").append(notificationEvent.getNotification().getTimestamp()).append("\n");
            strBid.append("Notification Message: ").append(notificationEvent.getNotification().getMessage()).append("\n");
        }

        log.info(strBid.toString());
    }
}

