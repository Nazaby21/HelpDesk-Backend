package com.zaby.helpdesk.controller;

import com.zaby.helpdesk.dto.request.DeviceTokenRequest;
import com.zaby.helpdesk.model.DeviceToken;
import com.zaby.helpdesk.model.User;
import com.zaby.helpdesk.repository.UserRepository;
import com.zaby.helpdesk.security.CustomUserDetails;
import com.zaby.helpdesk.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping({"/api/devices", "/api/v1/devices"})
@RequiredArgsConstructor
public class DeviceController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<Map<String, Object>> registerDevice(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Authentication authentication,
            Principal principal,
            @Valid @RequestBody DeviceTokenRequest request
    ) {
        User user = resolveCurrentUser(userDetails, authentication, principal);
        DeviceToken saved = notificationService.registerDeviceToken(user, request);
        return ResponseEntity.ok(Map.of(
                "id", saved.getId(),
                "userId", user.getId(),
                "token", saved.getToken(),
                "deviceToken", saved.getToken(),
                "platform", saved.getPlatform(),
                "message", "Device token registered successfully"
        ));
    }

    @DeleteMapping("/{token}")
    public ResponseEntity<Void> unregisterDevice(
            @PathVariable String token,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Authentication authentication,
            Principal principal
    ) {
        User user = resolveCurrentUserOptional(userDetails, authentication, principal);
        Long userId = user != null ? user.getId() : null;
        notificationService.unregisterDeviceToken(token, userId);
        return ResponseEntity.noContent().build();
    }

    private User resolveCurrentUser(
            CustomUserDetails userDetails,
            Authentication authentication,
            Principal principal
    ) {
        User user = resolveCurrentUserOptional(userDetails, authentication, principal);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return user;
    }

    private User resolveCurrentUserOptional(
            CustomUserDetails userDetails,
            Authentication authentication,
            Principal principal
    ) {
        CustomUserDetails details = userDetails;
        if (details == null && authentication != null && authentication.getPrincipal() instanceof CustomUserDetails cud) {
            details = cud;
        } else if (details == null && principal instanceof Authentication auth && auth.getPrincipal() instanceof CustomUserDetails cud) {
            details = cud;
        } else if (details == null && SecurityContextHolder.getContext().getAuthentication() != null
                && SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof CustomUserDetails cud) {
            details = cud;
        }

        if (details != null && details.getUser() != null) {
            return userRepository.findById(details.getUser().getId()).orElse(details.getUser());
        }
        if (principal != null && principal.getName() != null) {
            return userRepository.findByEmail(principal.getName()).orElse(null);
        }
        return null;
    }
}
