package com.zaby.helpdesk.service;

import com.zaby.helpdesk.dto.request.LoginRequest;
import com.zaby.helpdesk.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse login(LoginRequest request);
}
