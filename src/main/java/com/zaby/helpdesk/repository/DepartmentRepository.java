package com.zaby.helpdesk.repository;

import com.zaby.helpdesk.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department,Long> {
    Optional<Department> findById(Long id);
}
