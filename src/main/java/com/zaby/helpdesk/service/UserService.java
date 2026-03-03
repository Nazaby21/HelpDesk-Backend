package com.zaby.helpdesk.service;

import com.zaby.helpdesk.dto.request.UserRequest;
import com.zaby.helpdesk.dto.response.UserResponse;

import java.util.List;

public interface UserService {
   UserResponse createUser(UserRequest userRequest);
   UserResponse updateUser(long id, UserRequest userRequest);
   UserResponse getUserById(long id);
   List<UserResponse> getAllUsers();
   UserResponse deleteUser(long id);
}
