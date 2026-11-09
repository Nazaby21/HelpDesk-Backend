package com.zaby.helpdesk.service;

import com.zaby.helpdesk.dto.request.DeviceTokenRequest;
import com.zaby.helpdesk.dto.response.NotificationResponse;
import com.zaby.helpdesk.event.TicketCreatedEvent;
import com.zaby.helpdesk.event.TicketResolvedEvent;
import com.zaby.helpdesk.model.DeviceToken;
import com.zaby.helpdesk.model.User;
import org.springframework.data.domain.Page;

import java.util.List;

public interface NotificationService {

    List<User> resolveTicketCreatedRecipients(TicketCreatedEvent event);

    List<User> resolveTicketResolvedRecipients(TicketResolvedEvent event);

    void notifyTicketCreated(TicketCreatedEvent event);

    void notifyTicketResolved(TicketResolvedEvent event);

    Page<NotificationResponse> getUserNotifications(Long userId, int page, int size, boolean unreadOnly);

    long getUnreadCount(Long userId);

    NotificationResponse markAsRead(Long notificationId, Long userId);

    int markAllAsRead(Long userId);

    void deleteNotification(Long notificationId, Long userId);

    DeviceToken registerDeviceToken(User user, DeviceTokenRequest request);

    void unregisterDeviceToken(String token, Long userId);
}
