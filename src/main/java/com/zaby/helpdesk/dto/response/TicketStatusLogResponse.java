package com.zaby.helpdesk.dto.response;

import java.time.LocalDateTime;

public record TicketStatusLogResponse(
        Long id,
        Long ticketId,
        String fromStatus,
        String toStatus,
        Long changedById,
        String changedByName,
        String remark,
        LocalDateTime changedAt
) {
}
