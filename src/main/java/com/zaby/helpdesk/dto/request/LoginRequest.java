package com.zaby.helpdesk.dto.request;

public record LoginRequest(
        String email,
        String password
) { }
