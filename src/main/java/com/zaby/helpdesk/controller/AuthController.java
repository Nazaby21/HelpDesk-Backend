package com.zaby.helpdesk.controller;

import com.zaby.helpdesk.dto.request.LoginRequest;
import com.zaby.helpdesk.dto.request.RefreshTokenRequest;
import com.zaby.helpdesk.dto.response.AuthResponse;
import com.zaby.helpdesk.security.CustomUserDetails;
import com.zaby.helpdesk.security.JwtService;
import com.zaby.helpdesk.security.RefreshTokenService;
import com.zaby.helpdesk.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response){
        AuthResponse authResponse = authService.login(request, response);
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/refresh")
    @Transactional
    public ResponseEntity<AuthResponse> refresh(
            @RequestBody(required = false) RefreshTokenRequest request,
            @CookieValue(name = "refreshToken", required = false) String cookieRefreshToken
    ) {
        String refreshToken = null;
        if (request != null && request.refreshToken() != null && !request.refreshToken().isBlank()) {
            refreshToken = request.refreshToken();
        } else if (cookieRefreshToken != null && !cookieRefreshToken.isBlank()) {
            refreshToken = cookieRefreshToken;
        }

        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        var tokenEntity = refreshTokenService.findByToken(refreshToken);

        if (tokenEntity == null || tokenEntity.isExpired()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        CustomUserDetails userDetails = new CustomUserDetails(tokenEntity.getUser());
        String newAccessToken = jwtService.generateAccessToken(userDetails);

        Long departmentId = (tokenEntity.getUser().getDepartment() != null)
                ? tokenEntity.getUser().getDepartment().getId()
                : null;

        return ResponseEntity.ok(new AuthResponse(
                tokenEntity.getUser().getId(),
                tokenEntity.getUser().getEmail(),
                "Token refreshed",
                tokenEntity.getUser().getFirstName(),
                tokenEntity.getUser().getLastName(),
                tokenEntity.getUser().getImageUrl(),
                departmentId,
                tokenEntity.getUser().getRole(),
                newAccessToken,
                tokenEntity.getToken()
        ));
    }

    @PostMapping("/logout")
    @Transactional
    public ResponseEntity<Void> logout(
            @RequestBody(required = false) RefreshTokenRequest request,
            @CookieValue(name = "refreshToken", required = false) String cookieRefreshToken,
            HttpServletResponse response
    ) {
        String tokenToDelete = null;
        if (request != null && request.refreshToken() != null && !request.refreshToken().isBlank()) {
            tokenToDelete = request.refreshToken();
        } else if (cookieRefreshToken != null && !cookieRefreshToken.isBlank()) {
            tokenToDelete = cookieRefreshToken;
        }

        if (tokenToDelete != null) {
            refreshTokenService.deleteByToken(tokenToDelete);
        }

        if (response != null) {
            Cookie clearCookie = new Cookie("refreshToken", "");
            clearCookie.setHttpOnly(true);
            clearCookie.setSecure(false);
            clearCookie.setPath("/");
            clearCookie.setMaxAge(0);
            response.addCookie(clearCookie);
        }

        return ResponseEntity.ok().build();
    }
}
