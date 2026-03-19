package com.zaby.helpdesk.controller;

import com.zaby.helpdesk.dto.request.LoginRequest;
import com.zaby.helpdesk.dto.request.RefreshTokenRequest;
import com.zaby.helpdesk.dto.response.AuthResponse;
import com.zaby.helpdesk.repository.RefreshTokenRepository;
import com.zaby.helpdesk.security.CustomUserDetails;
import com.zaby.helpdesk.security.JwtService;
import com.zaby.helpdesk.security.RefreshTokenService;
import com.zaby.helpdesk.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request, HttpServletResponse response){
        AuthResponse authResponse = authService.login(request, response);
        return ResponseEntity.ok(authResponse);
//        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody(required = false) RefreshTokenRequest request) {

        if (request == null || request.refreshToken() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String refreshToken = request.refreshToken();
        var tokenEntity = refreshTokenService.findByToken(refreshToken);

        if (tokenEntity == null || tokenEntity.isExpired()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        CustomUserDetails userDetails = new CustomUserDetails(tokenEntity.getUser());
        String newAccessToken = jwtService.generateAccessToken(userDetails);

        return ResponseEntity.ok(new AuthResponse(
                tokenEntity.getUser().getEmail(),
                "Token refreshed",
                tokenEntity.getUser().getFirstName(),
                tokenEntity.getUser().getLastName(),
                tokenEntity.getUser().getDepartment().getId(),
                tokenEntity.getUser().getRole(),
                newAccessToken,
                refreshToken
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody(required = false) RefreshTokenRequest request) {
        if (request != null && request.refreshToken() != null) {
            refreshTokenService.deleteByToken(request.refreshToken());
        }
        return ResponseEntity.ok().build();
    }
}
