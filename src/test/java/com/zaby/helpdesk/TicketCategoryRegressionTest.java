package com.zaby.helpdesk;

import com.zaby.helpdesk.model.Category;
import com.zaby.helpdesk.model.Ticket;
import com.zaby.helpdesk.repository.CategoryRepository;
import com.zaby.helpdesk.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.AuditorAware;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
public class TicketCategoryRegressionTest {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @MockitoBean
    private AuditorAware<Long> auditorProvider;

    @BeforeEach
    void setup() {
        org.mockito.Mockito.when(auditorProvider.getCurrentAuditor()).thenReturn(java.util.Optional.of(1L));
        ticketRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    void deleteTicketDoesNotDeleteCategory() {
        Category parent = new Category();
        parent.setName("Parent");
        parent = categoryRepository.save(parent);

        Ticket ticket = new Ticket();
        ticket.setTicketTitle("Test");
        ticket.setCategory(parent);
        ticket = ticketRepository.save(ticket);

        ticketRepository.delete(ticket);

        assertThat(categoryRepository.findById(parent.getId())).isPresent();
    }

    @Test
    void createTicketWithExistingCategoryWorks() {
        Category parent = new Category();
        parent.setName("Parent");
        parent = categoryRepository.save(parent);

        Ticket ticket = new Ticket();
        ticket.setTicketTitle("Test");
        ticket.setCategory(parent);
        ticket = ticketRepository.save(ticket);

        assertThat(ticket.getId()).isNotNull();
        assertThat(ticket.getCategory().getId()).isEqualTo(parent.getId());
    }

    @Test
    void updateTicketCategoryWorks() {
        Category parent1 = new Category();
        parent1.setName("Parent 1");
        parent1 = categoryRepository.save(parent1);

        Category parent2 = new Category();
        parent2.setName("Parent 2");
        parent2 = categoryRepository.save(parent2);

        Ticket ticket = new Ticket();
        ticket.setTicketTitle("Test");
        ticket.setCategory(parent1);
        ticket = ticketRepository.save(ticket);

        ticket.setCategory(parent2);
        ticket = ticketRepository.save(ticket);

        Ticket saved = ticketRepository.findById(ticket.getId()).get();
        assertThat(saved.getCategory().getId()).isEqualTo(parent2.getId());
    }
}


