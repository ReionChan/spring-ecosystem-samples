package io.github.reionchan.event;

import lombok.Builder;
import lombok.Data;
import org.springframework.cloud.bus.event.Destination;
import org.springframework.cloud.bus.event.RemoteApplicationEvent;

import java.io.Serializable;

/**
 * 通知远程应用事件
 *
 * @author Reion
 * @date 2023-11-11
 **/
@Data
public class NotificationRemoteApplicationEvent extends RemoteApplicationEvent {

    private Notification notification;

    public NotificationRemoteApplicationEvent() {
        // for serializers
        this.notification = null;
    }

    public NotificationRemoteApplicationEvent(Object source, String originService, Destination destination, Notification notification) {
        super(source, originService, destination);
        this.notification = notification;
    }

    @Data
    @Builder
    public static class Notification implements Serializable {
        private String id;
        private Long timestamp;
        private String message;
    }
}
