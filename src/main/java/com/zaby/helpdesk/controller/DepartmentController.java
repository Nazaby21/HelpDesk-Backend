package com.zaby.helpdesk.controller;

import com.zaby.helpdesk.dto.request.DepartmentRequest;
import com.zaby.helpdesk.dto.response.DepartmentResponse;
import com.zaby.helpdesk.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/departments")
public class DepartmentController {
    private final DepartmentService departmentService;

    // create department
    @PostMapping
    public ResponseEntity<DepartmentResponse> createDepartment(@RequestBody DepartmentRequest departmentRequest){
        DepartmentResponse departmentResponse = departmentService.createDepartment(departmentRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(departmentResponse);
    }

    // update department
    @PutMapping("{id}")
    public ResponseEntity<DepartmentResponse> updateDepartment(@RequestBody DepartmentRequest departmentRequest, @PathVariable Long id){
        DepartmentResponse departmentResponse = departmentService.updateDepartment(id, departmentRequest);
        return ResponseEntity.ok(departmentResponse);
    }

    // find department by id
    @GetMapping("{id}")
    public ResponseEntity<DepartmentResponse> getDepartmentById(@PathVariable Long id){
        DepartmentResponse departmentResponse = departmentService.getDepartmentById(id);
        return ResponseEntity.ok(departmentResponse);
    }

    // find all department
    @GetMapping
    public ResponseEntity<List<DepartmentResponse>> getAllDepartments(){
        List<DepartmentResponse> departmentResponseList = departmentService.getAllDepartments();
        return ResponseEntity.ok(departmentResponseList);
    }
}
