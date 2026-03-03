package com.zaby.helpdesk.dto.request;

import com.zaby.helpdesk.enumeration.Role;

public record UserRequest (
    String firstName,
    String lastName,
    String email,
    String phoneNumber,
    String password,
    String imageUrl,
    Role role,
    Long departmentId
){}
