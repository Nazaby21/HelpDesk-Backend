package com.zaby.helpdesk.controller;


import com.zaby.helpdesk.dto.response.TicketStatusLogResponse;
import com.zaby.helpdesk.service.TicketStatusLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/status_log")
@RequiredArgsConstructor
public class TicketStatusLogController {

    private final TicketStatusLogService ticketStatusLogService;

    @GetMapping("/ticket/{ticketId}")
    public ResponseEntity<List<TicketStatusLogResponse>> getLogsByTicket(@PathVariable Long ticketId){
        List<TicketStatusLogResponse> logs = ticketStatusLogService.getLogsByTicketId(ticketId);
        return ResponseEntity.ok(logs);
    }
}
