package com.zaby.helpdesk.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;

public record DeviceTokenRequest(
        String token,
        String deviceToken,
        String platform
) {
    @JsonIgnore
    public String resolvedToken() {
        if (token != null && !token.isBlank()) {
            return token.trim();
        }
        if (deviceToken != null && !deviceToken.isBlank()) {
            return deviceToken.trim();
        }
        return null;
    }

    @AssertTrue(message = "token or deviceToken must not be blank")
    @JsonIgnore
    public boolean isTokenPresent() {
        return resolvedToken() != null;
    }
}
