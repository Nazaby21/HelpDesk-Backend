package com.zaby.helpdesk.controller;

import com.zaby.helpdesk.dto.request.UserRequest;
import com.zaby.helpdesk.dto.response.UserResponse;
import com.zaby.helpdesk.security.CustomUserDetails;
import com.zaby.helpdesk.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    // create user
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest userRequest){
        UserResponse userResponse = userService.createUser(userRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(userResponse);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        // Extract user details from JWT
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        // Fetch full user info from DB if needed
        UserResponse userResponse = userService.getUserById(userDetails.getUser().getId());

        return ResponseEntity.ok(userResponse);
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateCurrentUser(
            Authentication authentication,
            @RequestBody java.util.Map<String, Object> updates
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        UserResponse userResponse = userService.updateCurrentUser(userDetails.getUser().getId(), updates);
        return ResponseEntity.ok(userResponse);
    }

    // find user by id
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id){
        UserResponse userResponse = userService.getUserById(id);
        return ResponseEntity.ok(userResponse);
    }

    // find all user (supports pagination if page or size is provided, else returns List for web client)
    @GetMapping
    public ResponseEntity<?> getAllUsers(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ){
        if (page != null || size != null) {
            int pageNum = page != null ? page : 0;
            int pageSize = size != null ? size : 10;
            Page<UserResponse> userPage = userService.getAllUsers(PageRequest.of(pageNum, pageSize));
            return ResponseEntity.ok(userPage);
        }
        List<UserResponse> userResponse = userService.getAllUsers();
        return ResponseEntity.ok(userResponse);
    }

    // Update user
    @PutMapping("{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id, @Valid @RequestBody UserRequest userRequest){
        UserResponse userResponse = userService.updateUser(id, userRequest);
        return ResponseEntity.ok(userResponse);
    }

    // soft delete
    @DeleteMapping("{id}")
    public ResponseEntity<UserResponse> deleteUser(@PathVariable Long id){
        UserResponse userResponse= userService.deleteUser(id);
        return ResponseEntity.ok(userResponse);
    }
}
