package com.zaby.helpdesk.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaby.helpdesk.dto.request.DeviceTokenRequest;
import com.zaby.helpdesk.dto.request.TicketRequest;
import com.zaby.helpdesk.dto.request.UpdateStatusRequest;
import com.zaby.helpdesk.enumeration.NotificationType;
import com.zaby.helpdesk.enumeration.Priority;
import com.zaby.helpdesk.enumeration.Role;
import com.zaby.helpdesk.enumeration.Status;
import com.zaby.helpdesk.model.Category;
import com.zaby.helpdesk.model.Notification;
import com.zaby.helpdesk.model.User;
import com.zaby.helpdesk.repository.*;
import com.zaby.helpdesk.security.CustomUserDetails;
import com.zaby.helpdesk.security.JwtService;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
class NotificationIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private DeviceTokenRepository deviceTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private User regularUser;
    private User technician1;
    private User technician2;
    private Category category;

    private String userJwt;
    private String tech1Jwt;
    private String tech2Jwt;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        notificationRepository.deleteAll();
        deviceTokenRepository.deleteAll();
        ticketRepository.deleteAll();

        regularUser = userRepository.findByEmail("user.test@helpdesk.com").orElseGet(() -> {
            User u = new User();
            u.setFirstName("John");
            u.setLastName("User");
            u.setEmail("user.test@helpdesk.com");
            u.setPassword(passwordEncoder.encode("password123"));
            u.setRole(Role.USER);
            return userRepository.save(u);
        });

        technician1 = userRepository.findByEmail("tech1.test@helpdesk.com").orElseGet(() -> {
            User u = new User();
            u.setFirstName("Sarah");
            u.setLastName("Technician");
            u.setEmail("tech1.test@helpdesk.com");
            u.setPassword(passwordEncoder.encode("password123"));
            u.setRole(Role.TECHNICIAN);
            return userRepository.save(u);
        });

        technician2 = userRepository.findByEmail("tech2.test@helpdesk.com").orElseGet(() -> {
            User u = new User();
            u.setFirstName("Mike");
            u.setLastName("Engineer");
            u.setEmail("tech2.test@helpdesk.com");
            u.setPassword(passwordEncoder.encode("password123"));
            u.setRole(Role.TECHNICIAN);
            return userRepository.save(u);
        });

        category = categoryRepository.findAll().stream().findFirst().orElseGet(() -> {
            Category c = new Category();
            c.setName("Hardware");
            c.setDescription("Hardware issues");
            return categoryRepository.save(c);
        });

        userJwt = "Bearer " + jwtService.generateAccessToken(new CustomUserDetails(regularUser));
        tech1Jwt = "Bearer " + jwtService.generateAccessToken(new CustomUserDetails(technician1));
        tech2Jwt = "Bearer " + jwtService.generateAccessToken(new CustomUserDetails(technician2));
    }

    @Test
    void createTicketAndResolveTicket_endToEndNotificationFlow() throws Exception {
        // 1. USER creates a ticket -> TECHNICIANs get TICKET_CREATED notification
        TicketRequest createRequest = new TicketRequest(
                "Wi-Fi Not Connecting",
                "Cannot connect to corporate Wi-Fi on 3rd floor",
                Priority.HIGH,
                category.getId(),
                null,
                null,
                List.of()
        );

        String ticketJson = mockMvc.perform(post("/api/v1/tickets")
                        .header("Authorization", userJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long ticketId = objectMapper.readTree(ticketJson).get("id").asLong();

        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            long tech1Unread = notificationRepository.countByRecipientIdAndIsReadFalse(technician1.getId());
            long tech2Unread = notificationRepository.countByRecipientIdAndIsReadFalse(technician2.getId());
            long creatorUnread = notificationRepository.countByRecipientIdAndIsReadFalse(regularUser.getId());

            assertThat(tech1Unread).isEqualTo(1L);
            assertThat(tech2Unread).isEqualTo(1L);
            assertThat(creatorUnread).isZero();
        });

        // Verify technician can list and check unread count via /api/notifications
        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", tech1Jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(1));

        String tech1ListJson = mockMvc.perform(get("/api/notifications")
                        .param("page", "0")
                        .param("size", "10")
                        .param("unreadOnly", "true")
                        .header("Authorization", tech1Jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value(NotificationType.TICKET_CREATED.name()))
                .andExpect(jsonPath("$.content[0].ticketId").value(ticketId))
                .andExpect(jsonPath("$.content[0].isRead").value(false))
                .andReturn().getResponse().getContentAsString();

        Long tech1NotifId = objectMapper.readTree(tech1ListJson).at("/content/0/id").asLong();

        // User cannot mark technician's notification as read (403 Forbidden)
        mockMvc.perform(patch("/api/notifications/" + tech1NotifId + "/read")
                        .header("Authorization", userJwt))
                .andExpect(status().isForbidden());

        // Technician marks notification as read
        mockMvc.perform(patch("/api/notifications/" + tech1NotifId + "/read")
                        .header("Authorization", tech1Jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRead").value(true))
                .andExpect(jsonPath("$.readAt").isNotEmpty());

        // 2. TECHNICIAN resolves the ticket -> USER who created the ticket gets TICKET_RESOLVED notification
        UpdateStatusRequest resolveRequest = new UpdateStatusRequest(
                Status.COMPLETED,
                "Replaced faulty access point on 3rd floor"
        );

        mockMvc.perform(patch("/api/v1/tickets/" + ticketId + "/status")
                        .header("Authorization", tech1Jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resolveRequest)))
                .andExpect(status().isOk());

        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            long creatorUnread = notificationRepository.countByRecipientIdAndIsReadFalse(regularUser.getId());
            assertThat(creatorUnread).isEqualTo(1L);
        });

        String userNotifJson = mockMvc.perform(get("/api/notifications")
                        .header("Authorization", userJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value(NotificationType.TICKET_RESOLVED.name()))
                .andExpect(jsonPath("$.content[0].ticketId").value(ticketId))
                .andExpect(jsonPath("$.content[0].message").value(org.hamcrest.Matchers.containsString("Sarah Technician")))
                .andExpect(jsonPath("$.content[0].message").value(org.hamcrest.Matchers.containsString("Replaced faulty access point on 3rd floor")))
                .andReturn().getResponse().getContentAsString();

        Long userNotifId = objectMapper.readTree(userNotifJson).at("/content/0/id").asLong();

        // Mark all as read for user
        mockMvc.perform(patch("/api/notifications/read-all")
                        .header("Authorization", userJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedCount").value(1));

        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", userJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(0));

        // Delete notification
        mockMvc.perform(delete("/api/notifications/" + userNotifId)
                        .header("Authorization", userJwt))
                .andExpect(status().isNoContent());

        assertThat(notificationRepository.findById(userNotifId)).isEmpty();
    }

    @Test
    void deviceTokenEndpoints_registerRefreshAndUnregister() throws Exception {
        DeviceTokenRequest registerReq = new DeviceTokenRequest("fcm-test-token-123", null, "ANDROID");

        mockMvc.perform(post("/api/devices")
                        .header("Authorization", userJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("fcm-test-token-123"))
                .andExpect(jsonPath("$.platform").value("ANDROID"));

        assertThat(deviceTokenRepository.findByToken("fcm-test-token-123")).isPresent();

        mockMvc.perform(delete("/api/devices/fcm-test-token-123")
                        .header("Authorization", userJwt))
                .andExpect(status().isNoContent());

        assertThat(deviceTokenRepository.findByToken("fcm-test-token-123")).isEmpty();
    }
}
