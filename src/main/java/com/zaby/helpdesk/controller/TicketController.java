package com.zaby.helpdesk.controller;

import com.zaby.helpdesk.dto.request.TicketRequest;
import com.zaby.helpdesk.dto.request.UpdateStatusRequest;
import com.zaby.helpdesk.dto.response.DashboardStatsResponse;
import com.zaby.helpdesk.dto.response.MonthlyChartResponse;
import com.zaby.helpdesk.dto.response.NotificationResponse;
import com.zaby.helpdesk.dto.response.TicketResponse;
import com.zaby.helpdesk.enumeration.Priority;
import com.zaby.helpdesk.enumeration.Status;
import com.zaby.helpdesk.security.CustomUserDetails;
import com.zaby.helpdesk.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    // create
    @PostMapping
    public ResponseEntity<TicketResponse> createTicket(@Valid @RequestBody TicketRequest ticketRequest){
        TicketResponse ticketResponse = ticketService.createTicket(ticketRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketResponse);
    }

    // find by id
    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> getById(@PathVariable Long id){
        TicketResponse ticketResponse = ticketService.getById(id);
        return ResponseEntity.ok(ticketResponse);
    }

    // find all active (with optional status, priority, search filtering)
    @GetMapping
    public ResponseEntity<Page<TicketResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) String search
    ){
        Page<TicketResponse> ticketResponses = ticketService.getAll(page, size, status, priority, search);
        return ResponseEntity.ok(ticketResponses);
    }

    // find history (completed - filtered by user role with optional status, priority, search filtering)
    @GetMapping("/history")
    public ResponseEntity<Page<TicketResponse>> getHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) String search
    ){
        Long userId = userDetails.getUser().getId();
        String role = userDetails.getAuthorities().iterator().next().getAuthority();
        Page<TicketResponse> ticketResponses = ticketService.getHistory(userId, role, page, size, status, priority, search);
        return ResponseEntity.ok(ticketResponses);
    }

    // find my tickets (created by current user, non-completed with optional status, priority, search filtering)
    @GetMapping("/my")
    public ResponseEntity<Page<TicketResponse>> getMyTickets(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) String search
    ){
        Long userId = userDetails.getUser().getId();
        Page<TicketResponse> ticketResponses = ticketService.getMyTickets(userId, page, size, status, priority, search);
        return ResponseEntity.ok(ticketResponses);
    }

    // update status
    @PatchMapping("/{id}/status")
    public ResponseEntity<TicketResponse> updateTicketStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest updateStatusRequest,
            @AuthenticationPrincipal CustomUserDetails userDetails){
        Long currentUserId = userDetails != null ? userDetails.getUser().getId() : null;
        TicketResponse ticketResponse = ticketService.updateTicketStatus(id, updateStatusRequest.status(), updateStatusRequest.remark(), currentUserId);
        return ResponseEntity.ok(ticketResponse);
    }

    // update ticket
    @PutMapping("/{id}")
    public ResponseEntity<TicketResponse> updateTicket(@PathVariable Long id, @Valid @RequestBody TicketRequest ticketRequest){
        TicketResponse ticketResponse = ticketService.updateTicket(id, ticketRequest);
        return ResponseEntity.ok(ticketResponse);
    }

    // delete ticket
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTicket(@PathVariable Long id) {
        ticketService.deletedTicket(id);
        return ResponseEntity.noContent().build();
    }

    // dashboard stats
    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsResponse> getStats(){
        return ResponseEntity.ok(ticketService.getDashboardStats());
    }

    // monthly stats
    @GetMapping("/stats/monthly")
    public ResponseEntity<MonthlyChartResponse> getMonthlyStats(@RequestParam(required = false) Integer year){
        if (year == null) {
            year = java.time.Year.now().getValue();
        }
        return ResponseEntity.ok(ticketService.getMonthlyStats(year));
    }

    // activity feed
    @GetMapping("/activity")
    public ResponseEntity<Page<NotificationResponse>> getActivityFeed(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getUser().getId();
        String role = authentication.getAuthorities().iterator().next().getAuthority();

        return ResponseEntity.ok(ticketService.getActivityFeed(userId, role, page, size));
    }
}
