package com.zaby.helpdesk.mapper;


import com.zaby.helpdesk.dto.request.UserRequest;
import com.zaby.helpdesk.dto.response.UserResponse;
import com.zaby.helpdesk.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(source = "department.id", target = "departmentId")
    @Mapping(source = "department.name", target = "departmentName")
    UserResponse toResponse(User user);

    @Mapping(source = "departmentId", target = "department.id")
    User toEntity(UserRequest userRequest);
}
