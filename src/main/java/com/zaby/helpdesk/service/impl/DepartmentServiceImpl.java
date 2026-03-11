package com.zaby.helpdesk.service.impl;


import com.zaby.helpdesk.dto.request.DepartmentRequest;
import com.zaby.helpdesk.dto.response.DepartmentResponse;
import com.zaby.helpdesk.mapper.DepartmentMapper;
import com.zaby.helpdesk.model.Department;
import com.zaby.helpdesk.repository.DepartmentRepository;
import com.zaby.helpdesk.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final DepartmentMapper departmentMapper;

    @Override
    public DepartmentResponse createDepartment(DepartmentRequest departmentRequest) {
        Department department = departmentMapper.toEntity(departmentRequest);
        Department savedDepartment = departmentRepository.save(department);
        return departmentMapper.toDepartmentResponse(savedDepartment);
    }

    @Override
    public DepartmentResponse updateDepartment(Long id, DepartmentRequest departmentRequest) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Department not found with id: " + id));

        department.setName(departmentRequest.name());
        department.setDescription(departmentRequest.description());

        Department updatedDepartment = departmentRepository.save(department);
        return departmentMapper.toDepartmentResponse(updatedDepartment);
    }

    @Override
    public DepartmentResponse getDepartmentById(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Department not found with id: " + id));
        return departmentMapper.toDepartmentResponse(department);
    }

    @Override
    public List<DepartmentResponse> getAllDepartments() {
        return departmentRepository.findAll()
                .stream()
                .map(departmentMapper::toDepartmentResponse)
                .toList();
    }
}
