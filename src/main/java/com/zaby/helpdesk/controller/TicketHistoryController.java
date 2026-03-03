package com.zaby.helpdesk.controller;

import com.zaby.helpdesk.dto.response.TicketHistoryResponse;
import com.zaby.helpdesk.service.TicketHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/history")
@RequiredArgsConstructor
public class TicketHistoryController {

    private final TicketHistoryService ticketHistoryService;

    @GetMapping("/ticket/{ticketId}")
    public ResponseEntity<List<TicketHistoryResponse>> getHistoryByTicket(@PathVariable Long ticketId) {
        List<TicketHistoryResponse> history = ticketHistoryService.findByTicketId(ticketId);
        return ResponseEntity.ok(history);
    }
}
