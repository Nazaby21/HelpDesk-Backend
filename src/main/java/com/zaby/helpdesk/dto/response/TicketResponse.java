package com.zaby.helpdesk.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zaby.helpdesk.enumeration.Priority;
import com.zaby.helpdesk.enumeration.Status;

import java.time.LocalDateTime;
import java.util.List;

public record TicketResponse (
        Long id,
        String ticketTitle,
        String description,
        Priority priority,
        Status status,
        Long categoryId,
        String categoryName,
        Long subCategoryId,
        String subCategoryName,
        Long assignedTo,
        String assignedName,
        List<TicketStatusLogResponse> logs,
        Long createdBy,
        String createdByName,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime createdAt,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime completedAt,
        List<String> imageUrls
){}
