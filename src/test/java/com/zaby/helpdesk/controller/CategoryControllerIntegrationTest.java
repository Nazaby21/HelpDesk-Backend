package com.zaby.helpdesk.controller;

import com.zaby.helpdesk.model.Category;
import com.zaby.helpdesk.model.Ticket;
import com.zaby.helpdesk.repository.CategoryRepository;
import com.zaby.helpdesk.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.zaby.helpdesk.exception.GlobalExceptionHandler;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.AuditorAware;

@ActiveProfiles("test")
@SpringBootTest
public class CategoryControllerIntegrationTest {
    @Test
    @WithMockUser
    void testGetCategories() throws Exception {
        mockMvc.perform(get("/api/v1/categories"))
               .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
               .andExpect(status().isOk());
    }

    private MockMvc mockMvc;

    @MockitoBean
    private AuditorAware<Long> auditorProvider;

    @Autowired
    private CategoryController categoryController;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @BeforeEach
    void setup() {
        org.mockito.Mockito.when(auditorProvider.getCurrentAuditor()).thenReturn(java.util.Optional.of(1L));

        this.mockMvc = MockMvcBuilders.standaloneSetup(categoryController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        ticketRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteEmptyCategory_Returns204() throws Exception {
        Category category = new Category();
        category.setName("Empty Category");
        category = categoryRepository.save(category);

        mockMvc.perform(delete("/api/v1/categories/" + category.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteCategoryWithSubIssues_NoTickets_Succeeds() throws Exception {
        Category parent = new Category();
        parent.setName("Parent");
        parent = categoryRepository.save(parent);

        Category sub = new Category();
        sub.setName("Sub");
        sub.setParent(parent);
        categoryRepository.save(sub);

        mockMvc.perform(delete("/api/v1/categories/" + parent.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteCategoryReferencedByTicket_Returns400() throws Exception {
        Category parent = new Category();
        parent.setName("Parent");
        parent = categoryRepository.save(parent);

        Category sub = new Category();
        sub.setName("Sub");
        sub.setParent(parent);
        sub = categoryRepository.save(sub);

        Ticket ticket = new Ticket();
        ticket.setTicketTitle("Test Ticket");
        ticket.setCategory(parent);
        ticket.setSubCategory(sub);
        ticketRepository.save(ticket);

        mockMvc.perform(delete("/api/v1/categories/" + parent.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot delete: used by 1 tickets"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteCategoryReferencedOnlyBySubIssue_Returns400() throws Exception {
        Category parent = new Category();
        parent.setName("Parent");
        parent = categoryRepository.save(parent);

        Category sub = new Category();
        sub.setName("Sub");
        sub.setParent(parent);
        sub = categoryRepository.save(sub);

        // Ticket only references the sub-category, not the parent category
        Ticket ticket = new Ticket();
        ticket.setTicketTitle("Test Ticket 2");
        ticket.setSubCategory(sub);
        ticketRepository.save(ticket);

        // Deleting the parent should still fail because its sub-issue is in use
        mockMvc.perform(delete("/api/v1/categories/" + parent.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot delete: used by 1 tickets"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteNonexistentCategory_Returns404() throws Exception {
        mockMvc.perform(delete("/api/v1/categories/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Category not found"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void unexpectedException_Returns500() throws Exception {
        // Create a separate MockMvc instance with a mock controller/service for this specific test
        // to force a generic Exception without breaking the real beans.
        com.zaby.helpdesk.service.CategoryService mockService = org.mockito.Mockito.mock(com.zaby.helpdesk.service.CategoryService.class);
        org.mockito.Mockito.doThrow(new RuntimeException("Database exploded!")).when(mockService).deleteCategory(999L);
        
        CategoryController mockController = new CategoryController(mockService);
        MockMvc exceptionMockMvc = MockMvcBuilders.standaloneSetup(mockController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        exceptionMockMvc.perform(delete("/api/v1/categories/999"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }
}




