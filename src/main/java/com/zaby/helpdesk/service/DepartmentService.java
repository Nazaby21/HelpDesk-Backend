package com.zaby.helpdesk.service;

import com.zaby.helpdesk.dto.request.DepartmentRequest;
import com.zaby.helpdesk.dto.response.DepartmentResponse;
import java.util.List;

public interface DepartmentService {
    DepartmentResponse createDepartment(DepartmentRequest departmentRequest);
    DepartmentResponse updateDepartment(Long id, DepartmentRequest departmentRequest);
    DepartmentResponse getDepartmentById(Long id);
    List<DepartmentResponse> getAllDepartments();
}
