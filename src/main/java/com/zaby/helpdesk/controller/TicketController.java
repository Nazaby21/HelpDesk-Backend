package com.zaby.helpdesk.controller;

import com.zaby.helpdesk.dto.request.TicketRequest;
import com.zaby.helpdesk.dto.request.UpdateStatusRequest;
import com.zaby.helpdesk.dto.response.TicketResponse;
import com.zaby.helpdesk.security.CustomUserDetails;
import com.zaby.helpdesk.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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

    // find all active
    @GetMapping
    public ResponseEntity<Page<TicketResponse>> getAll(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size){
        Page<TicketResponse> ticketResponses = ticketService.getAll(page, size);
        return ResponseEntity.ok(ticketResponses);
    }

    // find history (completed)
    @GetMapping("/history")
    public ResponseEntity<Page<TicketResponse>> getHistory(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size){
        Page<TicketResponse> ticketResponses = ticketService.getHistory(page, size);
        return ResponseEntity.ok(ticketResponses);
    }

    // find my tickets (created by current user, non-completed)
    @GetMapping("/my")
    public ResponseEntity<Page<TicketResponse>> getMyTickets(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size){
        Long userId = userDetails.getUser().getId();
        Page<TicketResponse> ticketResponses = ticketService.getMyTickets(userId, page, size);
        return ResponseEntity.ok(ticketResponses);
    }

    // update status
    @PatchMapping("/{id}/status")
    public ResponseEntity<TicketResponse> updateTicketStatus(
            @PathVariable Long id,
            @RequestBody UpdateStatusRequest updateStatusRequest,
            @AuthenticationPrincipal CustomUserDetails userDetails){
        Long currentUserId = userDetails != null ? userDetails.getUser().getId() : null;
        TicketResponse ticketResponse = ticketService.updateTicketStatus(id, updateStatusRequest.status(), updateStatusRequest.remark(), currentUserId);
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
