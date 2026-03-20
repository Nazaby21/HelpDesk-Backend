package com.zaby.helpdesk.dto.response;

import com.zaby.helpdesk.enumeration.Action;

import java.time.LocalDateTime;

public record TicketHistoryResponse (
    Long id,
    Long ticketId,
    String ticketTitle,
    Action action,
    String comment,
    Long performById,
    String performByName,
    LocalDateTime performAt
) { }
