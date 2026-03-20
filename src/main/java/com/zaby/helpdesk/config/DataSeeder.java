package com.zaby.helpdesk.config;

import com.zaby.helpdesk.enumeration.Role;
import com.zaby.helpdesk.model.Department;
import com.zaby.helpdesk.model.User;
import com.zaby.helpdesk.repository.DepartmentRepository;
import com.zaby.helpdesk.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedDepartments();
        seedAdminUser();
    }

    private void seedDepartments() {
        if (departmentRepository.count() == 0) {
            Department it = new Department();
            it.setName("IT");
            it.setDescription("Information Technology");
            departmentRepository.save(it);

            Department hr = new Department();
            hr.setName("HR");
            hr.setDescription("Human Resources");
            departmentRepository.save(hr);

            Department finance = new Department();
            finance.setName("Finance");
            finance.setDescription("Finance & Accounting");
            departmentRepository.save(finance);

            log.info("✅ Seeded default departments: IT, HR, Finance");
        }
    }

    private void seedAdminUser() {
        String adminEmail = "admin1@helpdesk.com";

        var existingAdmin = userRepository.findByEmail(adminEmail);
        if (existingAdmin.isEmpty()) {
            Department itDept = departmentRepository.findAll()
                    .stream()
                    .filter(d -> "IT".equals(d.getName()))
                    .findFirst()
                    .orElse(null);

            User admin = new User();
            admin.setFirstName("Admin");
            admin.setLastName("User");
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode("admin1234"));
            admin.setRole(Role.ADMIN);
            admin.setDepartment(itDept);

            userRepository.save(admin);
            log.info("✅ Seeded admin user: {} / admin123", adminEmail);
        } else {
            // Ensure password is correct (may have been corrupted in a previous failed deploy)
            User admin = existingAdmin.get();
            admin.setPassword(passwordEncoder.encode("admin1234"));
            userRepository.save(admin);
            log.info("ℹ️ Admin user already exists, password reset to default.");
        }
    }
}
