package com.zaby.helpdesk.controller;

import com.zaby.helpdesk.dto.response.NotificationResponse;
import com.zaby.helpdesk.model.User;
import com.zaby.helpdesk.repository.UserRepository;
import com.zaby.helpdesk.security.CustomUserDetails;
import com.zaby.helpdesk.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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
@RequestMapping({"/api/notifications", "/api/v1/notifications"})
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> listNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Authentication authentication,
            Principal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "false") boolean unreadOnly
    ) {
        Long userId = resolveCurrentUserId(userDetails, authentication, principal);
        return ResponseEntity.ok(notificationService.getUserNotifications(userId, page, size, unreadOnly));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Authentication authentication,
            Principal principal
    ) {
        Long userId = resolveCurrentUserId(userDetails, authentication, principal);
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of("unreadCount", count, "count", count));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Authentication authentication,
            Principal principal
    ) {
        Long userId = resolveCurrentUserId(userDetails, authentication, principal);
        return ResponseEntity.ok(notificationService.markAsRead(id, userId));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Map<String, Object>> markAllAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Authentication authentication,
            Principal principal
    ) {
        Long userId = resolveCurrentUserId(userDetails, authentication, principal);
        int updated = notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(Map.of(
                "updatedCount", updated,
                "message", "All notifications marked as read"
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Authentication authentication,
            Principal principal
    ) {
        Long userId = resolveCurrentUserId(userDetails, authentication, principal);
        notificationService.deleteNotification(id, userId);
        return ResponseEntity.noContent().build();
    }

    private Long resolveCurrentUserId(
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

        if (details != null && details.getUser() != null && details.getUser().getId() != null) {
            return details.getUser().getId();
        }
        if (principal != null && principal.getName() != null) {
            User user = userRepository.findByEmail(principal.getName()).orElse(null);
            if (user != null) {
                return user.getId();
            }
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
    }
}
