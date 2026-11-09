package com.zaby.helpdesk.service.notification;

import com.zaby.helpdesk.model.Notification;
import com.zaby.helpdesk.model.User;
import com.zaby.helpdesk.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class InAppNotificationChannel implements NotificationChannel {

    private final NotificationRepository notificationRepository;

    @Override
    public void send(Notification notification, User recipient) {
        Notification saved = notificationRepository.save(notification);
        notification.setId(saved.getId());
        notification.setCreatedAt(saved.getCreatedAt());
        log.debug("Persisted in-app notification id={} for recipientId={}", saved.getId(), recipient.getId());
    }
}
