package com.zaby.helpdesk.service;

import com.zaby.helpdesk.dto.request.LoginRequest;
import com.zaby.helpdesk.dto.response.AuthResponse;
import com.zaby.helpdesk.model.User;

public interface AuthService {
    AuthResponse login(LoginRequest request);
}
