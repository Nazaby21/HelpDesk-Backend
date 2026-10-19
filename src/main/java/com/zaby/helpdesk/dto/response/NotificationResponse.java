package com.zaby.helpdesk.dto.response;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        String image,
        String title,
        String subTitle,
        LocalDateTime createdAt
) {}
