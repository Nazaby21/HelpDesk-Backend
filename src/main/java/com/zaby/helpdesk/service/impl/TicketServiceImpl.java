package com.zaby.helpdesk.service.impl;

import com.zaby.helpdesk.dto.request.TicketRequest;
import com.zaby.helpdesk.dto.response.TicketResponse;
import com.zaby.helpdesk.enumeration.Action;
import com.zaby.helpdesk.enumeration.Status;
import com.zaby.helpdesk.mapper.TicketMapper;
import com.zaby.helpdesk.model.*;
import com.zaby.helpdesk.repository.CategoryRepository;
import com.zaby.helpdesk.repository.TicketHistoryRepository;
import com.zaby.helpdesk.repository.TicketRepository;
import com.zaby.helpdesk.repository.UserRepository;
import com.zaby.helpdesk.service.TicketService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {


    private final TicketMapper ticketMapper;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final TicketHistoryRepository ticketHistoryRepository;
    private final CategoryRepository categoryRepository;

    private static final Map<Status, List<Status>> allowedTransitions = Map.of(
            Status.PENDING, List.of(Status.IN_PROGRESS),
            Status.IN_PROGRESS, List.of(Status.COMPLETED),
            Status.COMPLETED, List.of()
    );

    private void validateStatusTransition(Status from, Status to) {
        if (!allowedTransitions.containsKey(from)) {
            throw new IllegalArgumentException("Invalid transition from " + from + " to " + to);
        }
    }

    @Override
    @Transactional
    public TicketResponse createTicket(TicketRequest ticketRequest) {
        // 1. Convert Request DTO -> Entity
        Ticket ticket = ticketMapper.toEntity(ticketRequest);

        // status
        ticket.setStatus(Status.PENDING);

        // set category
        Category category = categoryRepository.findById(ticketRequest.categoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));
        ticket.setCategory(category);

        // set assigned user
        if (ticketRequest.assignedTo() != null) {
            User assignedUser = userRepository.findById(ticketRequest.assignedTo())
                    .orElseThrow(() -> new RuntimeException("User Not Found"));
            ticket.setAssignedTo(assignedUser);
        }

        // create initial StatusLog
        TicketStatusLog ticketStatusLog = new TicketStatusLog();
        ticketStatusLog.setTicket(ticket);
        ticketStatusLog.setFromStatus(null);
        ticketStatusLog.setToStatus(Status.PENDING.name());
        ticketStatusLog.setRemark("Ticket Created");
        ticketStatusLog.setChangedAt(LocalDateTime.now());

        ticket.getTicketStatusLog().add(ticketStatusLog); // add to ticket

        // create initial history
        TicketHistory  ticketHistory = new TicketHistory();
        ticketHistory.setTicket(ticket);
        ticketHistory.setAction(Action.CREATE);
        ticketHistory.setComment("Ticket Created");
        ticketHistory.setPerformAt(LocalDateTime.now());

        ticketHistoryRepository.save(ticketHistory);

        Ticket savedTicket = ticketRepository.save(ticket); // save ticket
        return ticketMapper.toTicketResponse(savedTicket);
    }

    @Override
    @Transactional
    public TicketResponse updateTicket(Long id, TicketRequest ticketRequest) {
        // check ticket
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket Not Found"));

        // validate ticket after status change to complete
        if(ticket.getStatus() == Status.COMPLETED) {
            throw new IllegalArgumentException("Completed ticket cannot be modified");
        }

        // update ticket
        ticket.setTicketTitle(ticketRequest.ticketTitle());
        ticket.setDescription(ticketRequest.description());
        ticket.setPriority(ticketRequest.priority());

        // Update category if provided
        if (ticketRequest.categoryId() != null) {
            Category category = categoryRepository.findById(ticketRequest.categoryId())
                    .orElseThrow(() -> new RuntimeException("Category Not Found"));
            ticket.setCategory(category);
        }

        // Update assigned user if provided
        if (ticketRequest.assignedTo() != null) {
            User assignedUser = userRepository.findById(ticketRequest.assignedTo())
                    .orElseThrow(() -> new RuntimeException("User Not Found"));
            ticket.setAssignedTo(assignedUser);
        }

        // add history
        TicketHistory ticketHistory = new TicketHistory();
        ticketHistory.setTicket(ticket);
        ticketHistory.setAction(Action.UPDATE);
        ticketHistory.setComment("Ticket information updated");
        ticketHistory.setPerformAt(LocalDateTime.now());

        ticket.getTicketHistory().add(ticketHistory);

        Ticket savedTicket = ticketRepository.save(ticket);
        return ticketMapper.toTicketResponse(savedTicket);
    }

    @Override
    @Transactional
    public TicketResponse updateTicketStatus(Long id, Status status, String remark) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket Not Found"));

        Status currentStatus = ticket.getStatus();

        // cant update if it completed
        if(currentStatus == Status.COMPLETED) {
            throw new IllegalArgumentException("Completed ticket cannot be modified");
        }

        // validate status
        validateStatusTransition(currentStatus, status);

        // update status
        ticket.setStatus(status);

        // create status log
        TicketStatusLog log = new TicketStatusLog();
        log.setTicket(ticket);
        log.setFromStatus(currentStatus.name());
        log.setToStatus(status.name());
        log.setRemark(remark);
        log.setChangedAt(LocalDateTime.now());

        ticket.getTicketStatusLog().add(log);

        // add history
        TicketHistory history = new TicketHistory();
        history.setTicket(ticket);
        history.setAction(Action.UPDATE);
        history.setComment("Status changed from " + currentStatus + " to " + status);
        history.setPerformAt(LocalDateTime.now());

        ticket.getTicketHistory().add(history);
        Ticket savedTicket = ticketRepository.save(ticket);

        return ticketMapper.toTicketResponse(savedTicket);
    }

    @Override
    public TicketResponse deletedTicket(Long id) {
        Ticket  ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket Not Found"));

        if (ticket.getStatus() == Status.COMPLETED) {
            throw new IllegalArgumentException("Completed ticket cannot be deleted");
        }

        ticket.setDeleted(true);

        // add history
        TicketHistory ticketHistory = new TicketHistory();
        ticketHistory.setTicket(ticket);
        ticketHistory.setAction(Action.DELETE);
        ticketHistory.setComment("Ticket Deleted");
        ticketHistory.setPerformAt(LocalDateTime.now());

        ticket.getTicketHistory().add(ticketHistory);

        Ticket savedTicket = ticketRepository.save(ticket);
        return ticketMapper.toTicketResponse(savedTicket);
    }

    @Override
    public TicketResponse getById(Long id) {
        Ticket  ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket Not Found"));

        return  ticketMapper.toTicketResponse(ticket);
    }

    @Override
    public Page<TicketResponse> getAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Ticket> tickets = ticketRepository.findAll(pageable);
        return tickets.map(ticketMapper::toTicketResponse);
    }


}
