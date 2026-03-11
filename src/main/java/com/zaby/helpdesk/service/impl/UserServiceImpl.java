package com.zaby.helpdesk.service.impl;

import com.zaby.helpdesk.dto.request.UserRequest;
import com.zaby.helpdesk.dto.response.UserResponse;
import com.zaby.helpdesk.mapper.UserMapper;
import com.zaby.helpdesk.model.Department;
import com.zaby.helpdesk.model.User;
import com.zaby.helpdesk.repository.DepartmentRepository;
import com.zaby.helpdesk.repository.UserRepository;
import com.zaby.helpdesk.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final DepartmentRepository departmentRepository;

    @Override
    public UserResponse createUser(UserRequest userRequest) {
        // duplicate email
        if (userRepository.existsByEmail(userRequest.email())){
            throw new RuntimeException("Email already exists");
        }

        // DTO -> Entity
        User user = userMapper.toEntity(userRequest);

        // encode password
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // set department
        Department department = departmentRepository.findById(userRequest.departmentId())
                .orElseThrow(() -> new RuntimeException("Department not found"));
        user.setDepartment(department);

        // save
        userRepository.save(user);
        return userMapper.toResponse(user);
    }

    @Override
    public UserResponse updateUser(long id, UserRequest userRequest) {
        User user = userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setFirstName(userRequest.firstName());
        user.setLastName(userRequest.lastName());
        user.setPhoneNumber(userRequest.phoneNumber());
        user.setImageUrl(userRequest.imageUrl());
        user.setRole(userRequest.role());

        // find department
        Department department = departmentRepository.findById(userRequest.departmentId())
                .orElseThrow(() -> new RuntimeException("Department not found"));

        user.setDepartment(department);

        if(userRequest.password() != null){
            user.setPassword(passwordEncoder.encode(userRequest.password()));
        }

        userRepository.save(user);
        return userMapper.toResponse(user);
    }

    @Override
    public UserResponse getUserById(long id) {
        User user =  userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return userMapper.toResponse(user);
    }

    @Override
    public List<UserResponse> getAllUsers() {
        return userRepository.findAllByDeletedFalse()
                .stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Override
    public UserResponse deleteUser(long id) {
        User user = userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setDeleted(true);
        userRepository.save(user);
        return userMapper.toResponse(user);
    }
}
