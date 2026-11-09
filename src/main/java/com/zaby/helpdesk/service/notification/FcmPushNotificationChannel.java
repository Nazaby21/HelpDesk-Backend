package com.zaby.helpdesk.service.notification;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.zaby.helpdesk.model.DeviceToken;
import com.zaby.helpdesk.model.Notification;
import com.zaby.helpdesk.model.User;
import com.zaby.helpdesk.repository.DeviceTokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@Order(3)
public class FcmPushNotificationChannel implements NotificationChannel {

    private final DeviceTokenRepository deviceTokenRepository;
    private final ObjectProvider<FirebaseMessaging> firebaseMessagingProvider;

    @Value("${notifications.fcm.enabled:false}")
    private boolean fcmEnabled = false;

    public FcmPushNotificationChannel(
            DeviceTokenRepository deviceTokenRepository,
            ObjectProvider<FirebaseMessaging> firebaseMessagingProvider
    ) {
        this.deviceTokenRepository = deviceTokenRepository;
        this.firebaseMessagingProvider = firebaseMessagingProvider;
    }

    @Override
    public void send(Notification notification, User recipient) {
        FirebaseMessaging firebaseMessaging = firebaseMessagingProvider.getIfAvailable();
        if (!fcmEnabled && firebaseMessaging == null) {
            return;
        }
        if (firebaseMessaging == null) {
            log.debug("FCM is enabled in config, but FirebaseMessaging bean is not available (missing credentials). Skipping push.");
            return;
        }

        List<DeviceToken> tokens = deviceTokenRepository.findByUser_Id(recipient.getId());
        if (tokens.isEmpty()) {
            return;
        }

        String ticketLink = notification.getTicketId() != null ? "/tickets/" + notification.getTicketId() : "/notifications";

        for (DeviceToken deviceToken : tokens) {
            String rawToken = deviceToken.getToken();
            if (rawToken == null || rawToken.isBlank()) {
                continue;
            }
            try {
                Message message = Message.builder()
                        .setToken(rawToken)
                        .setNotification(com.google.firebase.messaging.Notification.builder()
                                .setTitle(notification.getTitle())
                                .setBody(notification.getMessage())
                                .build())
                        .putData("notificationId", notification.getId() != null ? String.valueOf(notification.getId()) : "")
                        .putData("ticketId", notification.getTicketId() != null ? String.valueOf(notification.getTicketId()) : "")
                        .putData("type", notification.getType() != null ? notification.getType().name() : "")
                        .putData("link", ticketLink)
                        .build();

                firebaseMessaging.send(message);
                log.debug("Sent FCM push notification to token for userId={}", recipient.getId());
            } catch (FirebaseMessagingException ex) {
                if (isInvalidOrExpiredToken(ex)) {
                    log.warn("Removing invalid/expired FCM device token for userId={}: {}", recipient.getId(), ex.getMessagingErrorCode());
                    try {
                        deviceTokenRepository.deleteByToken(rawToken);
                    } catch (Exception deleteEx) {
                        log.error("Failed to remove invalid FCM device token", deleteEx);
                    }
                } else {
                    log.error("Failed to send FCM push notification for userId={}: {}", recipient.getId(), ex.getMessage());
                }
            } catch (Exception ex) {
                log.error("Unexpected error sending FCM push notification for userId={}", recipient.getId(), ex);
            }
        }
    }

    private boolean isInvalidOrExpiredToken(FirebaseMessagingException ex) {
        MessagingErrorCode code = ex.getMessagingErrorCode();
        if (code == MessagingErrorCode.UNREGISTERED
                || code == MessagingErrorCode.INVALID_ARGUMENT
                || code == MessagingErrorCode.SENDER_ID_MISMATCH) {
            return true;
        }
        String errCode = ex.getErrorCode() != null ? ex.getErrorCode().name() : "";
        return "NOT_FOUND".equalsIgnoreCase(errCode) || "INVALID_ARGUMENT".equalsIgnoreCase(errCode);
    }
}
