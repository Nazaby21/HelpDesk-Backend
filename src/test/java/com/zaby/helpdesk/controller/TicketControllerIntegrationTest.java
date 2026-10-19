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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.AuditorAware;

import java.util.List;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
public class TicketControllerIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private TicketController ticketController;

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

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(ticketController)
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
    void testGetMonthlyStats_EmptyData() throws Exception {
        mockMvc.perform(get("/api/v1/tickets/stats/monthly")
                        .param("year", "2026")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.received", hasSize(12)))
                .andExpect(jsonPath("$.due", hasSize(12)))
                .andExpect(jsonPath("$.received[0]", is(0)))
                .andExpect(jsonPath("$.due[0]", is(0)));
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    void testGetMonthlyStats_WithData() throws Exception {
        Category category = new Category();
        category.setName("Hardware");
        category = categoryRepository.save(category);

        org.mockito.Mockito.when(auditorProvider.getCurrentAuditor()).thenReturn(java.util.Optional.of(testUser.getId()));
        
        Ticket t1 = new Ticket();
        t1.setTicketTitle("Ticket 1");
        t1.setDescription("Desc 1");
        t1.setStatus(Status.COMPLETED); // maps to 'received'
        t1.setPriority(Priority.LOW);
        t1.setCreatedBy(testUser.getId());
        t1.setCategory(category);
        ticketRepository.save(t1);

        Ticket t2 = new Ticket();
        t2.setTicketTitle("Ticket 2");
        t2.setDescription("Desc 2");
        t2.setStatus(Status.PENDING); // maps to 'due'
        t2.setPriority(Priority.HIGH);
        t2.setCreatedBy(testUser.getId());
        t2.setCategory(category);
        ticketRepository.save(t2);
        
        int currentMonth = LocalDateTime.now().getMonthValue();
        int currentYear = LocalDateTime.now().getYear();

        mockMvc.perform(get("/api/v1/tickets/stats/monthly")
                        .param("year", String.valueOf(currentYear))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.received[" + (currentMonth - 1) + "]", is(1)))
                .andExpect(jsonPath("$.due[" + (currentMonth - 1) + "]", is(1)));
    }
}
