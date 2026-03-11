package com.zaby.helpdesk.service.impl;

import com.zaby.helpdesk.dto.request.LoginRequest;
import com.zaby.helpdesk.dto.response.AuthResponse;
import com.zaby.helpdesk.model.User;
import com.zaby.helpdesk.security.CustomUserDetails;
import com.zaby.helpdesk.security.JwtService;
import com.zaby.helpdesk.security.RefreshTokenService;
import com.zaby.helpdesk.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class AuthServiceImpl implements AuthService {

    private final @Lazy AuthenticationManager authenticationManager;
    private final JwtService jwtService; // Add this
    private final RefreshTokenService refreshTokenService; // Add this

    @Override
    public AuthResponse login(LoginRequest request) {

        // Authenticate user
        var authToken = new UsernamePasswordAuthenticationToken(request.email(), request.password());
        var auth = authenticationManager.authenticate(authToken);

        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();

        // Generate JWT access token
        String accessToken = jwtService.generateAccessToken(userDetails);

        // Generate refresh token and store in DB
        var refreshToken = refreshTokenService.createRefreshToken(userDetails.getUser());

        // Return tokens with user info
        return new AuthResponse(
                "Login successful",
                userDetails.getUsername(),
                userDetails.getFirstName(),
                userDetails.getLastName(),
                userDetails.getDepartmentId(),
                accessToken,
                refreshToken.getToken()
        );
    }
}
