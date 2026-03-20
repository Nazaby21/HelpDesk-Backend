package com.zaby.helpdesk.dto.response;

import com.zaby.helpdesk.enumeration.Role;

import java.time.LocalDateTime;

public record UserResponse (
        Long id,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        String imageUrl,
        Role role,
        Long departmentId,
        String departmentName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
){}
