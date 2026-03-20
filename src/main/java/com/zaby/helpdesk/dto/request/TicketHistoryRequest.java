package com.zaby.helpdesk.dto.request;

import com.zaby.helpdesk.enumeration.Action;

public record TicketHistoryRequest(
        Long ticketId,
        Action action,
        String comment
) { }
