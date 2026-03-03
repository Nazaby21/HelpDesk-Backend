package com.zaby.helpdesk.dto.response;

import com.zaby.helpdesk.enumeration.Priority;
import com.zaby.helpdesk.model.TicketStatusLog;

import java.time.LocalDateTime;
import java.util.List;

public record TicketResponse (
        Long id,
        String ticketTitle,
        String description,
        Priority priority,
        Long categoryId,
        String categoryName,
        Long assignedTo,
        String assignedName,
        List<TicketStatusLog> logs,
        Long createdBy,
        LocalDateTime createdAt
){}
