package com.zaby.helpdesk.dto.response;

import com.zaby.helpdesk.enumeration.Role;

public record AuthResponse(
        String email,
        String message,
        String firstName,
        String lastName,
        Long departmentId,
        Role role,
        String accessToken,
        String refreshToken
) {
}
