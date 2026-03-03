package com.zaby.helpdesk.service.impl;

import com.zaby.helpdesk.dto.request.TicketRequest;
import com.zaby.helpdesk.dto.response.TicketResponse;
import com.zaby.helpdesk.enumeration.Action;
import com.zaby.helpdesk.enumeration.Status;
import com.zaby.helpdesk.mapper.TicketMapper;
import com.zaby.helpdesk.model.*;
import com.zaby.helpdesk.repository.TicketHistoryRepository;
import com.zaby.helpdesk.repository.TicketRepository;
import com.zaby.helpdesk.repository.UserRepository;
import com.zaby.helpdesk.service.TicketService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {


    private final TicketMapper ticketMapper;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final TicketHistoryRepository ticketHistoryRepository;


    @Override
    @Transactional
    public ResponseEntity<TicketResponse> createTicket(TicketRequest ticketRequest) {
        // 1. Convert Request DTO -> Entity
        Ticket ticket = ticketMapper.toEntity(ticketRequest);

        // status
        ticket.setStatus(Status.PENDING);

//        Category category = categoryRepository

//        if (ticketRequest.assignedTo() != null) {
//            User assignedUser = userRepository.findById(ticketRequest.assignedTo())
//                    .orElseThrow(() -> new RuntimeException("User Not Found"));
//            ticket.setAssignedTo(assignedUser);
//        }

        // create initial StatusLog
        TicketStatusLog ticketStatusLog = new TicketStatusLog();
        ticketStatusLog.setTicket(ticket);
        ticketStatusLog.setFromStatus(null);
        ticketStatusLog.setToStatus(Status.PENDING.name());
        ticketStatusLog.setRemark("Ticket Created");
        ticketStatusLog.setChangedAt(LocalDateTime.now());

        ticket.getTicketStatusLog().add(ticketStatusLog); // add to ticket

        Ticket savedTicket = ticketRepository.save(ticket); // save ticket

        // create initial history
        TicketHistory  ticketHistory = new TicketHistory();
        ticketHistory.setTicket(ticket);
        ticketHistory.setAction(Action.CREATE);
        ticketHistory.setComment("Ticket Created");
        ticketHistory.setPerformAt(LocalDateTime.now());

        ticketHistoryRepository.save(ticketHistory);


        TicketResponse response = ticketMapper.toTicketResponse(savedTicket);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<TicketResponse> updateTicket(Long id, TicketRequest ticketRequest) {
        return null;
    }

    @Override
    public ResponseEntity<TicketResponse> deleteTicket(Long id) {
        return null;
    }

    @Override
    public ResponseEntity<TicketResponse> getById(Long id) {
        return null;
    }

    @Override
    public ResponseEntity<Page<TicketResponse>> getAll(int page, int size) {
        return null;
    }

//    @Override
//    public void createTicket1(TicketRequest ticketRequest) {
//
//        Ticket ticket = new Ticket();
//        ticket.
//        ticketMapper.toTicketRequest(ticketRequest);
//        ticketRepository.save(ticket);
//    }
}
