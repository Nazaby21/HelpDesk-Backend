package com.zaby.helpdesk.service;

import com.zaby.helpdesk.dto.request.TicketRequest;
import com.zaby.helpdesk.dto.response.TicketResponse;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;

public interface TicketService {
    ResponseEntity<TicketResponse> createTicket(TicketRequest ticketRequest);
    ResponseEntity<TicketResponse> updateTicket(Long id, TicketRequest ticketRequest);
    ResponseEntity<TicketResponse> deleteTicket(Long id);
    ResponseEntity<TicketResponse> getById(Long id);
    ResponseEntity<Page<TicketResponse>> getAll(int page, int size);
}
