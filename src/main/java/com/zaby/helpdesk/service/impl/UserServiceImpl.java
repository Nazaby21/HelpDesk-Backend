package com.zaby.helpdesk.service.impl;

import com.zaby.helpdesk.dto.request.UserRequest;
import com.zaby.helpdesk.dto.response.UserResponse;
import com.zaby.helpdesk.enumeration.Role;
import com.zaby.helpdesk.enumeration.Status;
import com.zaby.helpdesk.exception.BusinessException;
import com.zaby.helpdesk.exception.ResourceNotFoundException;
import com.zaby.helpdesk.mapper.UserMapper;
import com.zaby.helpdesk.model.Department;
import com.zaby.helpdesk.model.User;
import com.zaby.helpdesk.repository.DepartmentRepository;
import com.zaby.helpdesk.repository.UserRepository;
import com.zaby.helpdesk.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
        if (userRepository.existsByEmail(userRequest.email())){
            throw new BusinessException("Email already exists");
        }

        User user = userMapper.toEntity(userRequest);

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        if (userRequest.departmentId() != null) {
            Department department = departmentRepository.findById(userRequest.departmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
            user.setDepartment(department);
        }

        userRepository.save(user);
        return userMapper.toResponse(user);
    }

    @Override
    public UserResponse updateUser(long id, UserRequest userRequest) {
        User user = userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setFirstName(userRequest.firstName());
        user.setLastName(userRequest.lastName());
        user.setEmail(userRequest.email());
        user.setPhoneNumber(userRequest.phoneNumber());
        user.setImageUrl(userRequest.imageUrl());
        user.setRole(userRequest.role());

        if (userRequest.departmentId() != null) {
            Department department = departmentRepository.findById(userRequest.departmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
            user.setDepartment(department);
        } else {
            user.setDepartment(null);
        }

        if(userRequest.password() != null && !userRequest.password().trim().isEmpty()){
            user.setPassword(passwordEncoder.encode(userRequest.password()));
        }

        userRepository.save(user);
        return userMapper.toResponse(user);
    }

    @Override
    public UserResponse updateCurrentUser(long id, java.util.Map<String, Object> updates) {
        User user = userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (updates.containsKey("firstName") && updates.get("firstName") != null) {
            String firstName = updates.get("firstName").toString().trim();
            if (!firstName.isEmpty()) user.setFirstName(firstName);
        }
        if (updates.containsKey("lastName") && updates.get("lastName") != null) {
            String lastName = updates.get("lastName").toString().trim();
            if (!lastName.isEmpty()) user.setLastName(lastName);
        }
        if (updates.containsKey("phoneNumber")) {
            Object phone = updates.get("phoneNumber");
            user.setPhoneNumber(phone != null && !phone.toString().trim().isEmpty() ? phone.toString().trim() : null);
        }
        if (updates.containsKey("imageUrl")) {
            Object img = updates.get("imageUrl");
            user.setImageUrl(img != null && !img.toString().trim().isEmpty() ? img.toString().trim() : null);
        }
        if (updates.containsKey("password") && updates.get("password") != null) {
            String rawPassword = updates.get("password").toString().trim();
            if (!rawPassword.isEmpty()) {
                user.setPassword(passwordEncoder.encode(rawPassword));
            }
        }

        userRepository.save(user);
        return getUserById(id);
    }

    @Override
    public UserResponse getUserById(long id) {
        User user =  userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UserResponse response = userMapper.toResponse(user);
        
        long created = ticketRepository.countByCreatedByAndDeletedFalse(id);
        long assigned = ticketRepository.countByAssignedToIdAndDeletedFalse(id);
        long resolved = 0;
        long open = 0;

        List<Status> openStatuses= List.of(Status.PENDING, Status.IN_PROGRESS);

        if (user.getRole() == Role.USER) {
            resolved = ticketRepository.countByCreatedByAndStatusAndDeletedFalse(id,Status.COMPLETED);
            open = ticketRepository.countByCreatedByAndStatusInAndDeletedFalse(id, openStatuses);
        } else {
            resolved = ticketRepository.countByAssignedToIdAndStatusAndDeletedFalse(id, Status.COMPLETED);
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
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAllByDeletedFalse(pageable)
                .map(userMapper::toResponse);
    }

    @Override
    public UserResponse deleteUser(long id) {
        User user = userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setDeleted(true);
        userRepository.save(user);
        return userMapper.toResponse(user);
    }
}
