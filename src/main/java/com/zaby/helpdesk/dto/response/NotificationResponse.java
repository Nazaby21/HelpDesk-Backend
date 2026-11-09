package com.zaby.helpdesk.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.zaby.helpdesk.enumeration.NotificationType;
import com.zaby.helpdesk.model.Notification;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        Long recipientId,
        NotificationType type,
        String title,
        String message,
        String subTitle,
        String image,
        Long ticketId,
        String link,
        @JsonProperty("isRead")
        boolean isRead,
        LocalDateTime createdAt,
        LocalDateTime readAt
) {
    public NotificationResponse(
            Long id,
            String image,
            String title,
            String subTitle,
            LocalDateTime createdAt
    ) {
        this(
                id,
                null,
                null,
                title,
                subTitle,
                subTitle,
                image,
                null,
                null,
                false,
                createdAt,
                null
        );
    }

    public static NotificationResponse fromEntity(Notification notification) {
        String link = notification.getTicketId() != null ? "/tickets/" + notification.getTicketId() : null;
        return new NotificationResponse(
                notification.getId(),
                notification.getRecipientId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getMessage(),
                "/images/user/user-01.png",
                notification.getTicketId(),
                link,
                notification.isRead(),
                notification.getCreatedAt(),
                notification.getReadAt()
        );
    }
}
