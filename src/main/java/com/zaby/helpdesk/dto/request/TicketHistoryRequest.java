package com.zaby.helpdesk.dto.request;

import com.zaby.helpdesk.enumeration.Action;
import com.zaby.helpdesk.model.Ticket;

public record TicketHistoryRequest(
        Long ticketId,
        Action action,
        String comment
) { }
