package com.zaby.helpdesk.service.impl;

import com.zaby.helpdesk.dto.request.DeviceTokenRequest;
import com.zaby.helpdesk.dto.response.NotificationResponse;
import com.zaby.helpdesk.enumeration.DevicePlatform;
import com.zaby.helpdesk.enumeration.NotificationType;
import com.zaby.helpdesk.enumeration.Role;
import com.zaby.helpdesk.event.TicketCreatedEvent;
import com.zaby.helpdesk.event.TicketResolvedEvent;
import com.zaby.helpdesk.exception.ResourceNotFoundException;
import com.zaby.helpdesk.model.DeviceToken;
import com.zaby.helpdesk.model.Notification;
import com.zaby.helpdesk.model.User;
import com.zaby.helpdesk.repository.DeviceTokenRepository;
import com.zaby.helpdesk.repository.NotificationRepository;
import com.zaby.helpdesk.repository.UserRepository;
import com.zaby.helpdesk.service.NotificationService;
import com.zaby.helpdesk.service.notification.NotificationChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final UserRepository userRepository;
    private final List<NotificationChannel> notificationChannels;

    @Override
    @Transactional(readOnly = true)
    public List<User> resolveTicketCreatedRecipients(TicketCreatedEvent event) {
        List<User> candidates = new ArrayList<>();
        if (event.assignedTechnicianId() != null) {
            userRepository.findByIdAndDeletedFalse(event.assignedTechnicianId())
                    .ifPresent(candidates::add);
        } else {
            candidates.addAll(userRepository.findAllByRoleAndDeletedFalse(Role.TECHNICIAN));
        }

        return candidates.stream()
                .filter(user -> user != null && user.getId() != null)
                .filter(user -> event.creatorId() == null || !user.getId().equals(event.creatorId()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> resolveTicketResolvedRecipients(TicketResolvedEvent event) {
        if (event.creatorId() == null) {
            return List.of();
        }
        if (event.resolvedByUserId() != null && event.resolvedByUserId().equals(event.creatorId())) {
            return List.of();
        }
        return userRepository.findByIdAndDeletedFalse(event.creatorId())
                .map(List::of)
                .orElseGet(List::of);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyTicketCreated(TicketCreatedEvent event) {
        try {
            List<User> recipients = resolveTicketCreatedRecipients(event);
            if (recipients.isEmpty()) {
                return;
            }

            String requester = (event.requesterName() != null && !event.requesterName().isBlank())
                    ? event.requesterName()
                    : "User";
            String priority = event.priority() != null ? event.priority().name() : "MEDIUM";
            String link = "/tickets/" + event.ticketId();

            String title = String.format("New Ticket #%d: %s", event.ticketId(), event.ticketTitle());
            String message = String.format(
                    "Ticket #%d \"%s\" [Priority: %s] was created by %s. Link: %s",
                    event.ticketId(),
                    event.ticketTitle(),
                    priority,
                    requester,
                    link
            );

            for (User recipient : recipients) {
                Notification notification = Notification.builder()
                        .recipientId(recipient.getId())
                        .type(NotificationType.TICKET_CREATED)
                        .title(title)
                        .message(message)
                        .ticketId(event.ticketId())
                        .isRead(false)
                        .createdAt(LocalDateTime.now())
                        .build();

                dispatchToChannels(notification, recipient);
            }
        } catch (Exception ex) {
            log.error("Failed to process TicketCreatedEvent for ticketId={}", event.ticketId(), ex);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyTicketResolved(TicketResolvedEvent event) {
        try {
            List<User> recipients = resolveTicketResolvedRecipients(event);
            if (recipients.isEmpty()) {
                return;
            }

            String resolver = (event.resolvedByName() != null && !event.resolvedByName().isBlank())
                    ? event.resolvedByName()
                    : "Technician";
            String notePart = (event.resolutionNote() != null && !event.resolutionNote().isBlank())
                    ? " Note: " + event.resolutionNote() + "."
                    : "";
            String link = "/tickets/" + event.ticketId();

            String title = String.format("Ticket #%d Resolved: %s", event.ticketId(), event.ticketTitle());
            String message = String.format(
                    "Your ticket #%d \"%s\" was resolved by %s.%s Link: %s",
                    event.ticketId(),
                    event.ticketTitle(),
                    resolver,
                    notePart,
                    link
            );

            for (User recipient : recipients) {
                Notification notification = Notification.builder()
                        .recipientId(recipient.getId())
                        .type(NotificationType.TICKET_RESOLVED)
                        .title(title)
                        .message(message)
                        .ticketId(event.ticketId())
                        .isRead(false)
                        .createdAt(LocalDateTime.now())
                        .build();

                dispatchToChannels(notification, recipient);
            }
        } catch (Exception ex) {
            log.error("Failed to process TicketResolvedEvent for ticketId={}", event.ticketId(), ex);
        }
    }

    private void dispatchToChannels(Notification notification, User recipient) {
        for (NotificationChannel channel : notificationChannels) {
            try {
                channel.send(notification, recipient);
            } catch (Exception ex) {
                log.error("Notification channel {} failed for recipientId={}: {}",
                        channel.getClass().getSimpleName(), recipient.getId(), ex.getMessage());
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUserNotifications(Long userId, int page, int size, boolean unreadOnly) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        Page<Notification> notifications = unreadOnly
                ? notificationRepository.findByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(userId, pageable)
                : notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId, pageable);
        return notifications.map(NotificationResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByRecipientIdAndIsReadFalse(userId);
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        if (!notification.getRecipientId().equals(userId)) {
            throw new AccessDeniedException("You cannot modify another user's notification");
        }

        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
            notification = notificationRepository.save(notification);
        }
        return NotificationResponse.fromEntity(notification);
    }

    @Override
    @Transactional
    public int markAllAsRead(Long userId) {
        List<Notification> unread = notificationRepository.findByRecipientIdAndIsReadFalse(userId);
        if (unread.isEmpty()) {
            return 0;
        }
        LocalDateTime now = LocalDateTime.now();
        for (Notification notification : unread) {
            notification.setRead(true);
            notification.setReadAt(now);
        }
        notificationRepository.saveAll(unread);
        return unread.size();
    }

    @Override
    @Transactional
    public void deleteNotification(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        if (!notification.getRecipientId().equals(userId)) {
            throw new AccessDeniedException("You cannot delete another user's notification");
        }

        notificationRepository.delete(notification);
    }

    @Override
    @Transactional
    public DeviceToken registerDeviceToken(User user, DeviceTokenRequest request) {
        String rawToken = request.resolvedToken();
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException("Device token must not be blank");
        }

        DevicePlatform platform = DevicePlatform.fromString(request.platform());
        LocalDateTime now = LocalDateTime.now();

        DeviceToken deviceToken = deviceTokenRepository.findByToken(rawToken)
                .orElseGet(() -> DeviceToken.builder()
                        .token(rawToken)
                        .createdAt(now)
                        .build());

        deviceToken.setUser(user);
        deviceToken.setPlatform(platform.name());
        deviceToken.setLastUsedAt(now);
        deviceToken.setUpdatedAt(now);
        return deviceTokenRepository.save(deviceToken);
    }

    @Override
    @Transactional
    public void unregisterDeviceToken(String token, Long userId) {
        if (token == null || token.isBlank()) {
            return;
        }
        deviceTokenRepository.findByToken(token.trim()).ifPresent(dt -> {
            if (userId == null || dt.getUser() == null || userId.equals(dt.getUser().getId())) {
                deviceTokenRepository.delete(dt);
            }
        });
    }
}
