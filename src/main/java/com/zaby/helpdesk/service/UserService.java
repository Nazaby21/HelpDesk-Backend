package com.zaby.helpdesk.service;

import com.zaby.helpdesk.dto.request.UserRequest;
import com.zaby.helpdesk.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface UserService {
    UserResponse createUser(UserRequest userRequest);
    UserResponse updateUser(long id, UserRequest userRequest);
    UserResponse updateCurrentUser(long id, Map<String, Object> updates);
    UserResponse getUserById(long id);
    List<UserResponse> getAllUsers();
    Page<UserResponse> getAllUsers(Pageable pageable);
    UserResponse deleteUser(long id);
}
