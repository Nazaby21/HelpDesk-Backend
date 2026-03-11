package com.zaby.helpdesk.controller;

import com.zaby.helpdesk.dto.request.TicketRequest;
import com.zaby.helpdesk.dto.request.UpdateStatusRequest;
import com.zaby.helpdesk.dto.response.TicketResponse;
import com.zaby.helpdesk.enumeration.Status;
import com.zaby.helpdesk.model.Ticket;
import com.zaby.helpdesk.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    // create
    @PostMapping
    public ResponseEntity<TicketResponse> createTicket(@RequestBody TicketRequest ticketRequest){
        TicketResponse ticketResponse = ticketService.createTicket(ticketRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketResponse);
    }

    // find by id
    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> getById(@PathVariable Long id){
        TicketResponse ticketResponse = ticketService.getById(id);
        return ResponseEntity.ok(ticketResponse);
    }

    // find all
    @GetMapping
    public ResponseEntity<Page<TicketResponse>> getAll(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size){
        Page<TicketResponse> ticketResponses = ticketService.getAll(page, size);
        return ResponseEntity.ok(ticketResponses);

    }

    // update status
    @PatchMapping("/{id}/status")
    public ResponseEntity<TicketResponse> updateTicketStatus(
            @PathVariable Long id,
            @RequestBody UpdateStatusRequest updateStatusRequest){
        TicketResponse ticketResponse = ticketService.updateTicketStatus(id, updateStatusRequest.status(), updateStatusRequest.remark());
        return ResponseEntity.ok(ticketResponse);
    }

    // update ticket
    @PutMapping("/{id}")
    public ResponseEntity<TicketResponse> updateTicket(@RequestBody TicketRequest ticketRequest,@PathVariable Long id){
        TicketResponse ticketResponse = ticketService.updateTicket(id,  ticketRequest);
        return ResponseEntity.ok(ticketResponse);
    }

    // delete ticket
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTicket(@PathVariable Long id) {

        ticketService.deletedTicket(id);

        return ResponseEntity.noContent().build();
    }
}
