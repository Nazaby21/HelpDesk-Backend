package com.zaby.helpdesk.service.impl;

import com.zaby.helpdesk.dto.request.LoginRequest;
import com.zaby.helpdesk.dto.response.AuthResponse;
import com.zaby.helpdesk.model.User;
import com.zaby.helpdesk.security.CustomUserDetails;
import com.zaby.helpdesk.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final @Lazy AuthenticationManager authenticationManager;

    @Override
    public AuthResponse login(LoginRequest request) {
        var authToken = new UsernamePasswordAuthenticationToken(request.email(), request.password());
        var auth = authenticationManager.authenticate(authToken);

        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        return new AuthResponse(
                "Login successful",
                userDetails.getUsername(),
                userDetails.getFirstName(),
                userDetails.getLastName(),
                userDetails.getDepartmentId()
        );
    }
}
