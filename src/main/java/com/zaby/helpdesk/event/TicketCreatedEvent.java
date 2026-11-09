package com.zaby.helpdesk.event;

import com.zaby.helpdesk.enumeration.Priority;

public record TicketCreatedEvent(
        Long ticketId,
        String ticketTitle,
        Priority priority,
        Long creatorId,
        String requesterName,
        Long assignedTechnicianId,
        Long categoryId,
        Long subCategoryId
) {
}
