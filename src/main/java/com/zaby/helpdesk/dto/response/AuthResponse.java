package com.zaby.helpdesk.dto.response;

import com.zaby.helpdesk.enumeration.Role;

public record AuthResponse(
        Long id,
        String email,
        String message,
        String firstName,
        String lastName,
        String imageUrl,
        Long departmentId,
        Role role,
        String accessToken,
        String refreshToken
) {
}
