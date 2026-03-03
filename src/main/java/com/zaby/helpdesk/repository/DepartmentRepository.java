package com.zaby.helpdesk.repository;

import com.zaby.helpdesk.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department,Long> {
    Optional<Department> findById(Long id);
    Optional<Department> findByName(String name);
}
