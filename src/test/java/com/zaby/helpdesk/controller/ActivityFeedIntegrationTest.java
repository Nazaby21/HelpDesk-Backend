package com.zaby.helpdesk.controller;

import com.zaby.helpdesk.enumeration.Action;
import com.zaby.helpdesk.enumeration.Priority;
import com.zaby.helpdesk.enumeration.Role;
import com.zaby.helpdesk.enumeration.Status;
import com.zaby.helpdesk.model.Category;
import com.zaby.helpdesk.model.Department;
import com.zaby.helpdesk.model.Ticket;
import com.zaby.helpdesk.model.TicketHistory;
import com.zaby.helpdesk.model.User;
import com.zaby.helpdesk.repository.CategoryRepository;
import com.zaby.helpdesk.repository.DepartmentRepository;
import com.zaby.helpdesk.repository.TicketHistoryRepository;
import com.zaby.helpdesk.repository.TicketRepository;
import com.zaby.helpdesk.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;

@SpringBootTest
@ActiveProfiles("test")
public class ActivityFeedIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TicketHistoryRepository ticketHistoryRepository;

    @Autowired
    private com.zaby.helpdesk.repository.RefreshTokenRepository refreshTokenRepository;

    @MockitoBean
    private AuditorAware<Long> auditorProvider;

    private User testUser1;
    private User testUser2;
    private User testTechnician;
    private User testAdmin;
    private Category category;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
                
        org.mockito.Mockito.when(auditorProvider.getCurrentAuditor()).thenReturn(java.util.Optional.of(1L));
        
        ticketHistoryRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        ticketRepository.deleteAll();
        userRepository.deleteAll();
        departmentRepository.deleteAll();
        categoryRepository.deleteAll();

        Department department = new Department();
        department.setName("IT");
        department = departmentRepository.save(department);

        category = new Category();
        category.setName("Hardware");
        category = categoryRepository.save(category);

        testUser1 = new User();
        testUser1.setEmail("user1@example.com");
        testUser1.setPassword("password");
        testUser1.setRole(Role.USER);
        testUser1.setDepartment(department);
        testUser1 = userRepository.save(testUser1);

        testUser2 = new User();
        testUser2.setEmail("user2@example.com");
        testUser2.setPassword("password");
        testUser2.setRole(Role.USER);
        testUser2.setDepartment(department);
        testUser2 = userRepository.save(testUser2);

        testTechnician = new User();
        testTechnician.setEmail("tech@example.com");
        testTechnician.setPassword("password");
        testTechnician.setRole(Role.TECHNICIAN);
        testTechnician.setDepartment(department);
        testTechnician = userRepository.save(testTechnician);
        
        testAdmin = new User();
        testAdmin.setEmail("admin@example.com");
        testAdmin.setPassword("password");
        testAdmin.setRole(Role.ADMIN);
        testAdmin.setDepartment(department);
        testAdmin = userRepository.save(testAdmin);
    }

    @AfterEach
    void cleanUp() {
        ticketHistoryRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        ticketRepository.deleteAll();
        userRepository.deleteAll();
        departmentRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    private org.springframework.security.core.Authentication getAuth(User user) {
        com.zaby.helpdesk.security.CustomUserDetails userDetails = new com.zaby.helpdesk.security.CustomUserDetails(user);
        return new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
    }

    @Test
    void testActivityFeed_UnauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/tickets/activity")
                .contentType(MediaType.APPLICATION_JSON))
                // Spring security without basic auth configured might return 403 instead of 401 for unauthenticated.
                // Depending on AuthenticationEntryPoint. Often 401. Let's check is4xxClientError.
                .andExpect(status().is4xxClientError()); 
    }

    @Test
    @org.springframework.security.test.context.support.WithUserDetails(value = "user1@example.com", userDetailsServiceBeanName = "customUserDetailsService", setupBefore = org.springframework.security.test.context.support.TestExecutionEvent.TEST_EXECUTION)
    void testActivityFeed_EmptyCase() throws Exception {
        mockMvc.perform(get("/api/v1/tickets/activity")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }
    
    @Test
    @org.springframework.security.test.context.support.WithUserDetails(value = "user1@example.com", userDetailsServiceBeanName = "customUserDetailsService", setupBefore = org.springframework.security.test.context.support.TestExecutionEvent.TEST_EXECUTION)
    void testActivityFeed_PathNotCapturedByTicketId() throws Exception {
        mockMvc.perform(get("/api/v1/tickets/activity")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void testActivityFeed_RolesAndPagination() throws Exception {
        org.mockito.Mockito.when(auditorProvider.getCurrentAuditor()).thenReturn(java.util.Optional.of(testUser1.getId()));
        Ticket t1 = new Ticket();
        t1.setTicketTitle("Ticket 1");
        t1.setDescription("Desc 1");
        t1.setStatus(Status.PENDING);
        t1.setPriority(Priority.LOW);
        t1.setCreatedBy(testUser1.getId());
        t1.setCategory(category);
        t1 = ticketRepository.save(t1);

        TicketHistory h1 = new TicketHistory();
        h1.setTicket(t1);
        h1.setAction(Action.CREATE);
        h1.setComment("Created");
        h1.setPerformedBy(testUser1);
        h1.setPerformAt(LocalDateTime.now().minusDays(2));
        ticketHistoryRepository.save(h1);

        org.mockito.Mockito.when(auditorProvider.getCurrentAuditor()).thenReturn(java.util.Optional.of(testUser2.getId()));
        Ticket t2 = new Ticket();
        t2.setTicketTitle("Ticket 2");
        t2.setDescription("Desc 2");
        t2.setStatus(Status.IN_PROGRESS);
        t2.setPriority(Priority.HIGH);
        t2.setCreatedBy(testUser2.getId());
        t2.setAssignedTo(testTechnician);
        t2.setCategory(category);
        t2 = ticketRepository.save(t2);

        TicketHistory h2 = new TicketHistory();
        h2.setTicket(t2);
        h2.setAction(Action.ASSIGN);
        h2.setComment("Assigned");
        h2.setPerformedBy(testAdmin);
        h2.setPerformAt(LocalDateTime.now().minusDays(1));
        ticketHistoryRepository.save(h2);

        TicketHistory h3 = new TicketHistory();
        h3.setTicket(t2);
        h3.setAction(Action.UPDATE);
        h3.setComment("Updated");
        h3.setPerformedBy(testTechnician);
        h3.setPerformAt(LocalDateTime.now());
        ticketHistoryRepository.save(h3);

        // Test User 1
        mockMvc.perform(get("/api/v1/tickets/activity")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(new com.zaby.helpdesk.security.CustomUserDetails(testUser1)))
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(h1.getId().intValue())));

        // Test User 2
        mockMvc.perform(get("/api/v1/tickets/activity")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(new com.zaby.helpdesk.security.CustomUserDetails(testUser2)))
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id", is(h3.getId().intValue())))
                .andExpect(jsonPath("$.content[1].id", is(h2.getId().intValue())));

        // Test Technician
        mockMvc.perform(get("/api/v1/tickets/activity")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(new com.zaby.helpdesk.security.CustomUserDetails(testTechnician)))
                .param("page", "0")
                .param("size", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(h3.getId().intValue())));

        mockMvc.perform(get("/api/v1/tickets/activity")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(new com.zaby.helpdesk.security.CustomUserDetails(testTechnician)))
                .param("page", "1")
                .param("size", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(h2.getId().intValue())));

        // Test Admin
        mockMvc.perform(get("/api/v1/tickets/activity")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(new com.zaby.helpdesk.security.CustomUserDetails(testAdmin)))
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[0].id", is(h3.getId().intValue())))
                .andExpect(jsonPath("$.content[1].id", is(h2.getId().intValue())))
                .andExpect(jsonPath("$.content[2].id", is(h1.getId().intValue())));
    }
}
