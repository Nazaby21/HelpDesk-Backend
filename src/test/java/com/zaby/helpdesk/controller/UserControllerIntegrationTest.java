package com.zaby.helpdesk.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaby.helpdesk.enumeration.Priority;
import com.zaby.helpdesk.enumeration.Role;
import com.zaby.helpdesk.enumeration.Status;
import com.zaby.helpdesk.model.Category;
import com.zaby.helpdesk.model.Department;
import com.zaby.helpdesk.model.Ticket;
import com.zaby.helpdesk.model.User;
import com.zaby.helpdesk.repository.CategoryRepository;
import com.zaby.helpdesk.repository.DepartmentRepository;
import com.zaby.helpdesk.repository.TicketRepository;
import com.zaby.helpdesk.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.zaby.helpdesk.exception.GlobalExceptionHandler;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.AuditorAware;

@SpringBootTest
@ActiveProfiles("test")
public class UserControllerIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private UserController userController;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private com.zaby.helpdesk.repository.RefreshTokenRepository refreshTokenRepository;
    
    @MockitoBean
    private AuditorAware<Long> auditorProvider;

    private User testUser;
    private User testTechnician;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
                
        org.mockito.Mockito.when(auditorProvider.getCurrentAuditor()).thenReturn(java.util.Optional.of(1L));
        refreshTokenRepository.deleteAll();
        ticketRepository.deleteAll();
        userRepository.deleteAll();
        departmentRepository.deleteAll();
        categoryRepository.deleteAll();

        Department department = new Department();
        department.setName("IT");
        department.setDescription("IT Dept");
        department = departmentRepository.save(department);

        testUser = new User();
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setEmail("user@example.com");
        testUser.setPassword("password");
        testUser.setRole(Role.USER);
        testUser.setDepartment(department);
        testUser = userRepository.save(testUser);
        
        testTechnician = new User();
        testTechnician.setFirstName("Tech");
        testTechnician.setLastName("Guy");
        testTechnician.setEmail("tech@example.com");
        testTechnician.setPassword("password");
        testTechnician.setRole(Role.TECHNICIAN);
        testTechnician.setDepartment(department);
        testTechnician = userRepository.save(testTechnician);
    }

    @AfterEach
    void cleanUp() {
        refreshTokenRepository.deleteAll();
        ticketRepository.deleteAll();
        userRepository.deleteAll();
        departmentRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void testGetProfileStats_EmptyData() throws Exception {
        mockMvc.perform(get("/api/v1/users/" + testUser.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testUser.getId().intValue())))
                .andExpect(jsonPath("$.ticketsCreated", is(0)))
                .andExpect(jsonPath("$.ticketsAssigned", is(0)))
                .andExpect(jsonPath("$.ticketsResolved", is(0)))
                .andExpect(jsonPath("$.ticketsOpen", is(0)));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void testGetProfileStats_WithTickets_AsUser() throws Exception {
        Category category = new Category();
        category.setName("Hardware");
        category = categoryRepository.save(category);

        // Created by User, status PENDING (Open)
        org.mockito.Mockito.when(auditorProvider.getCurrentAuditor()).thenReturn(java.util.Optional.of(testUser.getId()));
        Ticket t1 = new Ticket();
        t1.setTicketTitle("Ticket 1");
        t1.setDescription("Desc 1");
        t1.setStatus(Status.PENDING);
        t1.setPriority(Priority.LOW);
        t1.setCreatedBy(testUser.getId());
        t1.setCategory(category);
        ticketRepository.save(t1);

        // Created by User, status COMPLETED (Resolved)
        Ticket t2 = new Ticket();
        t2.setTicketTitle("Ticket 2");
        t2.setDescription("Desc 2");
        t2.setStatus(Status.COMPLETED);
        t2.setPriority(Priority.HIGH);
        t2.setCreatedBy(testUser.getId());
        t2.setCategory(category);
        ticketRepository.save(t2);

        // Assigned to User, status IN_PROGRESS (Open)
        org.mockito.Mockito.when(auditorProvider.getCurrentAuditor()).thenReturn(java.util.Optional.of(testTechnician.getId()));
        Ticket t3 = new Ticket();
        t3.setTicketTitle("Ticket 3");
        t3.setDescription("Desc 3");
        t3.setStatus(Status.IN_PROGRESS);
        t3.setPriority(Priority.MEDIUM);
        t3.setCreatedBy(testTechnician.getId()); // someone else created it
        t3.setAssignedTo(testUser);
        t3.setCategory(category);
        ticketRepository.save(t3);

        mockMvc.perform(get("/api/v1/users/" + testUser.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketsCreated", is(2)))
                .andExpect(jsonPath("$.ticketsAssigned", is(1)))
                .andExpect(jsonPath("$.ticketsResolved", is(1)))
                .andExpect(jsonPath("$.ticketsOpen", is(1))); // as USER role, resolved/open applies to tickets they CREATED
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void testGetProfileStats_WithTickets_AsTechnician() throws Exception {
        Category category = new Category();
        category.setName("Software");
        category = categoryRepository.save(category);

        // Assigned to Technician, status COMPLETED
        org.mockito.Mockito.when(auditorProvider.getCurrentAuditor()).thenReturn(java.util.Optional.of(testUser.getId()));
        Ticket t1 = new Ticket();
        t1.setTicketTitle("Ticket 1");
        t1.setDescription("Desc 1");
        t1.setStatus(Status.COMPLETED);
        t1.setPriority(Priority.LOW);
        t1.setCreatedBy(testUser.getId());
        t1.setAssignedTo(testTechnician);
        t1.setCategory(category);
        ticketRepository.save(t1);

        // Created by Technician, status PENDING
        org.mockito.Mockito.when(auditorProvider.getCurrentAuditor()).thenReturn(java.util.Optional.of(testTechnician.getId()));
        Ticket t2 = new Ticket();
        t2.setTicketTitle("Ticket 2");
        t2.setDescription("Desc 2");
        t2.setStatus(Status.PENDING);
        t2.setPriority(Priority.HIGH);
        t2.setCreatedBy(testTechnician.getId());
        t2.setCategory(category);
        ticketRepository.save(t2);

        mockMvc.perform(get("/api/v1/users/" + testTechnician.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketsCreated", is(1)))
                .andExpect(jsonPath("$.ticketsAssigned", is(1)))
                .andExpect(jsonPath("$.ticketsResolved", is(1)))
                .andExpect(jsonPath("$.ticketsOpen", is(0))); // as TECH role, resolved/open applies to tickets ASSIGNED to them
    }
}
