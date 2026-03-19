package com.zaby.helpdesk.service.impl;

import com.zaby.helpdesk.dto.request.LoginRequest;
import com.zaby.helpdesk.dto.response.AuthResponse;
import com.zaby.helpdesk.model.User;
import com.zaby.helpdesk.security.CustomUserDetails;
import com.zaby.helpdesk.security.JwtService;
import com.zaby.helpdesk.security.RefreshTokenService;
import com.zaby.helpdesk.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
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
    public AuthResponse login(LoginRequest request, HttpServletResponse response) {

        // Authenticate user
        var authToken = new UsernamePasswordAuthenticationToken(request.email(), request.password());
        var auth = authenticationManager.authenticate(authToken);

        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        if (userDetails == null) throw new RuntimeException("Authentication failed");

        // Generate JWT access token
        String accessToken = jwtService.generateAccessToken(userDetails);

        // Generate refresh token and store in DB
        var refreshToken = refreshTokenService.createRefreshToken(userDetails.getUser());

        // Set refresh token as HTTP-only cookie
        Cookie refreshCookie = new Cookie("refreshToken", refreshToken.getToken());
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(false); // Changed to false for local testing
        refreshCookie.setPath("/");     // cookie accessible to all API endpoints
        refreshCookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
        response.addCookie(refreshCookie);

        // Return user info + access token + refresh token in body
        return new AuthResponse(
                userDetails.getUsername(), // email
                "Login successful",        // message
                userDetails.getFirstName(),
                userDetails.getLastName(),
                userDetails.getDepartmentId(),
                userDetails.getUser().getRole(),
                accessToken,
                refreshToken.getToken() // Send in body so frontend can store it
        );
    }
}
