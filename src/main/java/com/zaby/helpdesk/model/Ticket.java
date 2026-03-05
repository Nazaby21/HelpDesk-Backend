package com.zaby.helpdesk.model;

import com.zaby.helpdesk.enumeration.Priority;
import com.zaby.helpdesk.enumeration.Status;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity
@AllArgsConstructor
@Table(name = "tickets")
public class Ticket extends AuditableCustom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_title")
    private String ticketTitle;

    @Column(length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    private Status status;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL)
    private List<TicketStatusLog> ticketStatusLog = new ArrayList<>();

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL)
    private List<TicketHistory> ticketHistory = new ArrayList<>();

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private User assignedTo;
}
