package com.zaby.helpdesk.dto.request;

import jakarta.validation.constraints.NotBlank;

public record DeviceTokenRequest(
        @NotBlank(message = "deviceToken must not be blank")
        String deviceToken,
        String platform
) {
}
