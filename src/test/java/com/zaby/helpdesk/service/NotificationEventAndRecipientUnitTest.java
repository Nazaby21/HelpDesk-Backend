package com.zaby.helpdesk.service;

import com.zaby.helpdesk.enumeration.NotificationType;
import com.zaby.helpdesk.enumeration.Priority;
import com.zaby.helpdesk.enumeration.Role;
import com.zaby.helpdesk.event.NotificationEventListener;
import com.zaby.helpdesk.event.TicketCreatedEvent;
import com.zaby.helpdesk.event.TicketResolvedEvent;
import com.zaby.helpdesk.model.Notification;
import com.zaby.helpdesk.model.User;
import com.zaby.helpdesk.repository.DeviceTokenRepository;
import com.zaby.helpdesk.repository.NotificationRepository;
import com.zaby.helpdesk.repository.UserRepository;
import com.zaby.helpdesk.service.impl.NotificationServiceImpl;
import com.zaby.helpdesk.service.notification.InAppNotificationChannel;
import com.zaby.helpdesk.service.notification.NotificationChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationEventAndRecipientUnitTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private DeviceTokenRepository deviceTokenRepository;

    @Mock
    private UserRepository userRepository;

    private NotificationServiceImpl notificationService;
    private NotificationEventListener eventListener;

    private User requester;
    private User tech1;
    private User tech2;

    @BeforeEach
    void setUp() {
        requester = new User();
        requester.setId(10L);
        requester.setFirstName("Alice");
        requester.setLastName("Requester");
        requester.setEmail("alice@helpdesk.com");
        requester.setRole(Role.USER);

        tech1 = new User();
        tech1.setId(20L);
        tech1.setFirstName("Bob");
        tech1.setLastName("Tech");
        tech1.setEmail("bob@helpdesk.com");
        tech1.setRole(Role.TECHNICIAN);

        tech2 = new User();
        tech2.setId(21L);
        tech2.setFirstName("Carol");
        tech2.setLastName("Tech");
        tech2.setEmail("carol@helpdesk.com");
        tech2.setRole(Role.TECHNICIAN);

        NotificationChannel inAppChannel = new InAppNotificationChannel(notificationRepository);
        notificationService = new NotificationServiceImpl(
                notificationRepository,
                deviceTokenRepository,
                userRepository,
                List.of(inAppChannel)
        );
        eventListener = new NotificationEventListener(notificationService);
    }

    @Test
    void resolveTicketCreatedRecipients_unassignedTicket_notifiesAllActiveTechnicians() {
        when(userRepository.findAllByRoleAndDeletedFalse(Role.TECHNICIAN))
                .thenReturn(List.of(tech1, tech2));

        TicketCreatedEvent event = new TicketCreatedEvent(
                100L, "VPN Issue", Priority.HIGH, requester.getId(), "Alice Requester", null, 1L, null
        );

        List<User> recipients = notificationService.resolveTicketCreatedRecipients(event);

        assertThat(recipients).extracting(User::getId).containsExactly(20L, 21L);
    }

    @Test
    void resolveTicketCreatedRecipients_assignedTicket_notifiesOnlyAssignedTechnician() {
        when(userRepository.findByIdAndDeletedFalse(20L))
                .thenReturn(Optional.of(tech1));

        TicketCreatedEvent event = new TicketCreatedEvent(
                101L, "Laptop Screen Broken", Priority.MEDIUM, requester.getId(), "Alice Requester", 20L, 1L, null
        );

        List<User> recipients = notificationService.resolveTicketCreatedRecipients(event);

        assertThat(recipients).extracting(User::getId).containsExactly(20L);
        verify(userRepository, never()).findAllByRoleAndDeletedFalse(any());
    }

    @Test
    void resolveTicketCreatedRecipients_excludesActorWhoTriggeredAction() {
        when(userRepository.findAllByRoleAndDeletedFalse(Role.TECHNICIAN))
                .thenReturn(List.of(tech1, tech2));

        // tech1 creates a ticket; tech1 must NOT be notified about their own action
        TicketCreatedEvent event = new TicketCreatedEvent(
                102L, "Server Maintenance", Priority.LOW, tech1.getId(), "Bob Tech", null, 1L, null
        );

        List<User> recipients = notificationService.resolveTicketCreatedRecipients(event);

        assertThat(recipients).extracting(User::getId).containsExactly(21L);
    }

    @Test
    void resolveTicketResolvedRecipients_notifiesCreatorOnlyAndExcludesSelfResolution() {
        when(userRepository.findByIdAndDeletedFalse(requester.getId()))
                .thenReturn(Optional.of(requester));

        TicketResolvedEvent resolvedByTech = new TicketResolvedEvent(
                100L, "VPN Issue", requester.getId(), tech1.getId(), "Bob Tech", "Reset VPN profile"
        );
        List<User> recipients = notificationService.resolveTicketResolvedRecipients(resolvedByTech);
        assertThat(recipients).extracting(User::getId).containsExactly(requester.getId());

        // Self-resolution must not notify the person who triggered the action
        TicketResolvedEvent resolvedBySelf = new TicketResolvedEvent(
                100L, "VPN Issue", requester.getId(), requester.getId(), "Alice Requester", "Fixed myself"
        );
        assertThat(notificationService.resolveTicketResolvedRecipients(resolvedBySelf)).isEmpty();
    }

    @Test
    void eventListener_handlesTicketCreatedAndResolved_andNeverThrowsOnChannelFailure() {
        when(userRepository.findAllByRoleAndDeletedFalse(Role.TECHNICIAN))
                .thenReturn(List.of(tech1));
        when(userRepository.findByIdAndDeletedFalse(requester.getId()))
                .thenReturn(Optional.of(requester));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId(500L);
            return n;
        });

        TicketCreatedEvent createdEvent = new TicketCreatedEvent(
                200L, "Printer Offline", Priority.HIGH, requester.getId(), "Alice Requester", null, 1L, null
        );
        eventListener.handleTicketCreated(createdEvent);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(1)).save(captor.capture());
        Notification savedCreated = captor.getValue();
        assertThat(savedCreated.getRecipientId()).isEqualTo(tech1.getId());
        assertThat(savedCreated.getType()).isEqualTo(NotificationType.TICKET_CREATED);
        assertThat(savedCreated.getMessage())
                .contains("200")
                .contains("Printer Offline")
                .contains("HIGH")
                .contains("Alice Requester")
                .contains("/tickets/200");

        reset(notificationRepository);
        when(notificationRepository.save(any(Notification.class))).thenThrow(new RuntimeException("Simulated DB outage"));

        TicketResolvedEvent resolvedEvent = new TicketResolvedEvent(
                200L, "Printer Offline", requester.getId(), tech1.getId(), "Bob Tech", "Replaced toner"
        );
        // Must never throw even if channel throws
        assertThatCode(() -> eventListener.handleTicketResolved(resolvedEvent)).doesNotThrowAnyException();
    }
}
