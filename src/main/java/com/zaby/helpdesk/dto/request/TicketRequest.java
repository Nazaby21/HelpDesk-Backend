package com.zaby.helpdesk.dto.request;

import com.zaby.helpdesk.enumeration.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TicketRequest (
        @NotBlank
        String ticketTitle,
        @NotBlank
        String description,
        @NotNull
        Priority priority,
        Long categoryId,
        Long subCategoryId,
        Long assignedTo,
        String imageUrl
){}
