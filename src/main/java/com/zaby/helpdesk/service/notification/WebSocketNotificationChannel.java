package com.zaby.helpdesk.service.notification;

import com.zaby.helpdesk.dto.response.NotificationResponse;
import com.zaby.helpdesk.model.Notification;
import com.zaby.helpdesk.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class WebSocketNotificationChannel implements NotificationChannel {

    private final SimpMessagingTemplate simpMessagingTemplate;

    @Value("${notifications.websocket.enabled:true}")
    private boolean websocketEnabled = true;

    @Override
    public void send(Notification notification, User recipient) {
        if (!websocketEnabled) {
            return;
        }
        NotificationResponse payload = NotificationResponse.fromEntity(notification);
        if (recipient.getEmail() != null && !recipient.getEmail().isBlank()) {
            simpMessagingTemplate.convertAndSendToUser(
                    recipient.getEmail(),
                    "/queue/notifications",
                    payload
            );
        }
        if (recipient.getId() != null) {
            simpMessagingTemplate.convertAndSendToUser(
                    String.valueOf(recipient.getId()),
                    "/queue/notifications",
                    payload
            );
            simpMessagingTemplate.convertAndSend(
                    "/topic/notifications/" + recipient.getId(),
                    payload
            );
        }
        log.debug("Pushed real-time WebSocket notification id={} to recipientId={}", notification.getId(), recipient.getId());
    }
}
