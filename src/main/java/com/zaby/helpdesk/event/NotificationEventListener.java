package com.zaby.helpdesk.event;

import com.zaby.helpdesk.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleTicketCreated(TicketCreatedEvent event) {
        try {
            notificationService.notifyTicketCreated(event);
        } catch (Exception ex) {
            log.error("Error handling TicketCreatedEvent for ticketId={}", event.ticketId(), ex);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleTicketResolved(TicketResolvedEvent event) {
        try {
            notificationService.notifyTicketResolved(event);
        } catch (Exception ex) {
            log.error("Error handling TicketResolvedEvent for ticketId={}", event.ticketId(), ex);
        }
    }
}
