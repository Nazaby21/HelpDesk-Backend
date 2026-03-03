package com.zaby.helpdesk.mapper;

import com.zaby.helpdesk.dto.request.DepartmentRequest;
import com.zaby.helpdesk.dto.response.DepartmentResponse;
import com.zaby.helpdesk.model.Department;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DepartmentMapper {

    DepartmentResponse toDepartmentResponse(Department department);

    Department toEntity(DepartmentRequest departmentRequest);
}
