package com.zaby.helpdesk.controller;

import com.zaby.helpdesk.dto.request.DeviceTokenRequest;
import com.zaby.helpdesk.model.DeviceToken;
import com.zaby.helpdesk.model.User;
import com.zaby.helpdesk.repository.DeviceTokenRepository;
import com.zaby.helpdesk.repository.UserRepository;
import com.zaby.helpdesk.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationTokenController {

    private final DeviceTokenRepository deviceTokenRepository;
    private final UserRepository userRepository;

    @PostMapping("/device-token")
    @Transactional
    public ResponseEntity<?> registerDeviceToken(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Authentication authentication,
            Principal principal,
            @Valid @RequestBody DeviceTokenRequest request
    ) {
        CustomUserDetails details = userDetails;
        if (details == null && authentication != null && authentication.getPrincipal() instanceof CustomUserDetails cud) {
            details = cud;
        } else if (details == null && principal instanceof Authentication auth && auth.getPrincipal() instanceof CustomUserDetails cud) {
            details = cud;
        } else if (details == null && SecurityContextHolder.getContext().getAuthentication() != null && SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof CustomUserDetails cud) {
            details = cud;
        }

        User user = null;
        if (details != null && details.getUser() != null) {
            user = userRepository.findById(details.getUser().getId()).orElse(details.getUser());
        } else if (principal != null) {
            user = userRepository.findByEmail(principal.getName()).orElse(null);
        }

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        DeviceToken token = deviceTokenRepository.findByDeviceToken(request.deviceToken())
                .orElseGet(() -> DeviceToken.builder()
                        .deviceToken(request.deviceToken())
                        .createdAt(LocalDateTime.now())
                        .build());

        token.setUser(user);
        token.setPlatform(request.platform());
        token.setUpdatedAt(LocalDateTime.now());
        deviceTokenRepository.save(token);

        return ResponseEntity.ok(Map.of("message", "Device token registered successfully"));
    }

    @DeleteMapping("/device-token")
    @Transactional
    public ResponseEntity<Void> deleteDeviceToken(@RequestParam String deviceToken) {
        deviceTokenRepository.deleteByDeviceToken(deviceToken);
        return ResponseEntity.noContent().build();
    }
}
