package com.zaby.helpdesk.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaby.helpdesk.dto.request.DeviceTokenRequest;
import com.zaby.helpdesk.dto.request.LoginRequest;
import com.zaby.helpdesk.dto.request.RefreshTokenRequest;
import com.zaby.helpdesk.dto.request.TicketRequest;
import com.zaby.helpdesk.enumeration.Priority;
import com.zaby.helpdesk.enumeration.Role;
import com.zaby.helpdesk.enumeration.Status;
import com.zaby.helpdesk.exception.GlobalExceptionHandler;
import com.zaby.helpdesk.model.*;
import com.zaby.helpdesk.repository.*;
import com.zaby.helpdesk.security.CustomUserDetails;
import com.zaby.helpdesk.security.RefreshTokenService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.AuditorAware;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
public class MobileApiEndpointsIntegrationTest {

    private MockMvc authMockMvc;
    private MockMvc ticketMockMvc;
    private MockMvc notificationMockMvc;
    private MockMvc userMockMvc;
    private MockMvc chatMockMvc;

    @Autowired
    private AuthController authController;

    @Autowired
    private TicketController ticketController;

    @Autowired
    private NotificationTokenController notificationTokenController;

    @Autowired
    private UserController userController;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private DeviceTokenRepository deviceTokenRepository;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AuditorAware<Long> auditorProvider;

    @MockitoBean
    private SimpMessagingTemplate simpMessagingTemplate;

    private User testUserWithDept;
    private User testUserWithoutDept;
    private Department department;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        authMockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        ticketMockMvc = MockMvcBuilders.standaloneSetup(ticketController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        notificationMockMvc = MockMvcBuilders.standaloneSetup(notificationTokenController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        userMockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        ChatController chatController = new ChatController(chatMessageRepository, simpMessagingTemplate);
        chatMockMvc = MockMvcBuilders.standaloneSetup(chatController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        org.mockito.Mockito.when(auditorProvider.getCurrentAuditor()).thenReturn(java.util.Optional.of(1L));

        cleanDb();

        department = new Department();
        department.setName("Engineering");
        department.setDescription("Engineering Dept");
        department = departmentRepository.save(department);

        testCategory = new Category();
        testCategory.setName("Network");
        testCategory.setDescription("Network issues");
        testCategory = categoryRepository.save(testCategory);

        testUserWithDept = new User();
        testUserWithDept.setFirstName("Alice");
        testUserWithDept.setLastName("Smith");
        testUserWithDept.setEmail("alice@example.com");
        testUserWithDept.setPassword(passwordEncoder.encode("password123"));
        testUserWithDept.setRole(Role.USER);
        testUserWithDept.setImageUrl("https://example.com/avatar1.png");
        testUserWithDept.setDepartment(department);
        testUserWithDept = userRepository.save(testUserWithDept);

        testUserWithoutDept = new User();
        testUserWithoutDept.setFirstName("Bob");
        testUserWithoutDept.setLastName("Jones");
        testUserWithoutDept.setEmail("bob@example.com");
        testUserWithoutDept.setPassword(passwordEncoder.encode("password123"));
        testUserWithoutDept.setRole(Role.USER);
        testUserWithoutDept.setImageUrl("https://example.com/avatar2.png");
        testUserWithoutDept.setDepartment(null);
        testUserWithoutDept = userRepository.save(testUserWithoutDept);
    }

    @AfterEach
    void tearDown() {
        cleanDb();
    }

    private void cleanDb() {
        deviceTokenRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        chatMessageRepository.deleteAll();
        ticketRepository.deleteAll();
        userRepository.deleteAll();
        departmentRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    void testLogin_ReturnsUserIdAvatarAndBothTokens() throws Exception {
        LoginRequest request = new LoginRequest("alice@example.com", "password123");

        authMockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUserWithDept.getId()))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.imageUrl").value("https://example.com/avatar1.png"))
                .andExpect(jsonPath("$.departmentId").value(department.getId()))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    void testTokenRefresh_WithDepartment_ReturnsNewToken() throws Exception {
        RefreshToken token = refreshTokenService.createRefreshToken(testUserWithDept);

        RefreshTokenRequest request = new RefreshTokenRequest(token.getToken());

        authMockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUserWithDept.getId()))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.imageUrl").value("https://example.com/avatar1.png"))
                .andExpect(jsonPath("$.departmentId").value(department.getId()))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").value(token.getToken()));
    }

    @Test
    void testTokenRefresh_WithoutDepartment_DoesNotThrowNpe() throws Exception {
        // B-01 Regression Test: Null department on user should not crash refresh endpoint
        RefreshToken token = refreshTokenService.createRefreshToken(testUserWithoutDept);

        RefreshTokenRequest request = new RefreshTokenRequest(token.getToken());

        authMockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUserWithoutDept.getId()))
                .andExpect(jsonPath("$.departmentId").doesNotExist())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").value(token.getToken()));
    }

    @Test
    void testTokenRefresh_InvalidOrMissingToken_Returns401() throws Exception {
        authMockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest("non-existent-token"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testLogout_DeletesRefreshToken() throws Exception {
        RefreshToken token = refreshTokenService.createRefreshToken(testUserWithDept);
        assertTrue(refreshTokenRepository.findByToken(token.getToken()).isPresent());

        RefreshTokenRequest request = new RefreshTokenRequest(token.getToken());

        authMockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        assertFalse(refreshTokenRepository.findByToken(token.getToken()).isPresent());
    }

    @Test
    void testDeviceToken_RegisterAndDelete() throws Exception {
        DeviceTokenRequest request = new DeviceTokenRequest("fcm_test_device_token_123", "ANDROID");

        CustomUserDetails userDetails = new CustomUserDetails(testUserWithDept);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        notificationMockMvc.perform(post("/api/v1/notifications/device-token")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Device token registered successfully"));

        assertTrue(deviceTokenRepository.findByDeviceToken("fcm_test_device_token_123").isPresent());

        // Delete device token
        notificationMockMvc.perform(delete("/api/v1/notifications/device-token")
                        .param("deviceToken", "fcm_test_device_token_123"))
                .andExpect(status().isNoContent());

        assertFalse(deviceTokenRepository.findByDeviceToken("fcm_test_device_token_123").isPresent());
    }

    @Test
    void testConsistentErrorFormat_OnValidationFailure() throws Exception {
        // Empty ticket title and null priority
        String invalidTicketPayload = "{\"ticketTitle\":\"\",\"description\":\"\",\"priority\":null}";

        ticketMockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidTicketPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors", hasSize(greaterThanOrEqualTo(2))));
    }

    @Test
    void testTickets_FilterByStatusPriorityAndSearch() throws Exception {
        Ticket t1 = new Ticket();
        t1.setTicketTitle("VPN connection lost in Tokyo office");
        t1.setDescription("Cannot connect to server");
        t1.setStatus(Status.PENDING);
        t1.setPriority(Priority.HIGH);
        t1.setCategory(testCategory);
        t1.setCreatedBy(testUserWithDept.getId());
        ticketRepository.save(t1);

        Ticket t2 = new Ticket();
        t2.setTicketTitle("Printer broken on floor 3");
        t2.setDescription("Paper jam error");
        t2.setStatus(Status.IN_PROGRESS);
        t2.setPriority(Priority.LOW);
        t2.setCategory(testCategory);
        t2.setCreatedBy(testUserWithDept.getId());
        ticketRepository.save(t2);

        // Filter by search="VPN"
        ticketMockMvc.perform(get("/api/v1/tickets")
                        .param("search", "VPN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].ticketTitle").value("VPN connection lost in Tokyo office"));

        // Filter by priority=HIGH
        ticketMockMvc.perform(get("/api/v1/tickets")
                        .param("priority", "HIGH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].priority").value("HIGH"));

        // Filter by status=IN_PROGRESS
        ticketMockMvc.perform(get("/api/v1/tickets")
                        .param("status", "IN_PROGRESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].status").value("IN_PROGRESS"));
    }

    @Test
    void testUsers_PaginationVsUnpaginated() throws Exception {
        // Without pagination parameters: returns raw List for web frontend backwards compatibility
        userMockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(2)));

        // With pagination parameters: returns Page object for mobile client
        userMockMvc.perform(get("/api/v1/users")
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void testChatMessages_PaginationVsUnpaginated() throws Exception {
        ChatMessage m1 = new ChatMessage();
        m1.setTicketId(100L);
        m1.setSenderId(1L);
        m1.setContent("Hello first message");
        m1.setTimestamp(LocalDateTime.now().minusMinutes(10));
        chatMessageRepository.save(m1);

        ChatMessage m2 = new ChatMessage();
        m2.setTicketId(100L);
        m2.setSenderId(2L);
        m2.setContent("Hello second message");
        m2.setTimestamp(LocalDateTime.now().minusMinutes(5));
        chatMessageRepository.save(m2);

        // Without pagination: returns raw List
        chatMockMvc.perform(get("/api/v1/tickets/100/messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(2)));

        // With pagination: returns Page object
        chatMockMvc.perform(get("/api/v1/tickets/100/messages")
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalElements").value(2));
    }
}
