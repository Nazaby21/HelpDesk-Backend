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
    private final com.zaby.helpdesk.repository.TicketRepository ticketRepository;

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
        user.setEmail(userRequest.email());
        user.setPhoneNumber(userRequest.phoneNumber());
        user.setImageUrl(userRequest.imageUrl());
        user.setRole(userRequest.role());

        // find department
        Department department = departmentRepository.findById(userRequest.departmentId())
                .orElseThrow(() -> new RuntimeException("Department not found"));

        user.setDepartment(department);

        if(userRequest.password() != null && !userRequest.password().trim().isEmpty()){
            user.setPassword(passwordEncoder.encode(userRequest.password()));
        }

        userRepository.save(user);
        return userMapper.toResponse(user);
    }

    @Override
    public UserResponse getUserById(long id) {
        User user =  userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserResponse response = userMapper.toResponse(user);
        
        long created = ticketRepository.countByCreatedByAndDeletedFalse(id);
        long assigned = ticketRepository.countByAssignedToIdAndDeletedFalse(id);
        long resolved = 0;
        long open = 0;
        
        java.util.List<com.zaby.helpdesk.enumeration.Status> openStatuses = java.util.List.of(
            com.zaby.helpdesk.enumeration.Status.PENDING, 
            com.zaby.helpdesk.enumeration.Status.IN_PROGRESS
        );

        if (user.getRole() == com.zaby.helpdesk.enumeration.Role.USER) {
            resolved = ticketRepository.countByCreatedByAndStatusAndDeletedFalse(id, com.zaby.helpdesk.enumeration.Status.COMPLETED);
            open = ticketRepository.countByCreatedByAndStatusInAndDeletedFalse(id, openStatuses);
        } else {
            resolved = ticketRepository.countByAssignedToIdAndStatusAndDeletedFalse(id, com.zaby.helpdesk.enumeration.Status.COMPLETED);
            open = ticketRepository.countByAssignedToIdAndStatusInAndDeletedFalse(id, openStatuses);
        }

        return new UserResponse(
                response.id(), response.firstName(), response.lastName(), response.email(),
                response.phoneNumber(), response.imageUrl(), response.role(),
                response.departmentId(), response.departmentName(),
                response.createdAt(), response.updatedAt(),
                created, assigned, resolved, open
        );
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
