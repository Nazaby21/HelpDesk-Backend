package com.zaby.helpdesk.dto.response;

public record AuthResponse(
        String email,
        String message,
        String firstName,
        String lastName,
        Long departmentId
) {
}
