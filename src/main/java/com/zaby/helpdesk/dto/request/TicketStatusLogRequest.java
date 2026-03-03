package com.zaby.helpdesk.dto.request;

public record TicketStatusLogRequest(
        String ticketId,
        String toStatus,
        String remark
) {
}
