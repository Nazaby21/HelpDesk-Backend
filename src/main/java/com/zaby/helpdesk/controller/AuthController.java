package com.zaby.helpdesk.controller;

import com.zaby.helpdesk.dto.request.LoginRequest;
import com.zaby.helpdesk.dto.response.AuthResponse;
import com.zaby.helpdesk.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request){
        AuthResponse authResponse = authService.login(request);
        return ResponseEntity.ok(authResponse);
//        return ResponseEntity.ok(authService.login(request));
    }
}
