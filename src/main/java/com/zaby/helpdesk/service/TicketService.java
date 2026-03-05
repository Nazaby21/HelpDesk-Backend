package com.zaby.helpdesk.service;

import com.zaby.helpdesk.dto.request.TicketRequest;
import com.zaby.helpdesk.dto.response.TicketResponse;
import com.zaby.helpdesk.enumeration.Status;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;

public interface TicketService {
    TicketResponse createTicket(TicketRequest ticketRequest);
    TicketResponse updateTicket(Long id, TicketRequest ticketRequest);
    TicketResponse updateTicketStatus(Long id, Status status, String remark);
    TicketResponse deleteTicket(Long id);
    TicketResponse getById(Long id);
    Page<TicketResponse>getAll(int page, int size);
//    Page<TicketResponse>getAll(int page, int size);
}
