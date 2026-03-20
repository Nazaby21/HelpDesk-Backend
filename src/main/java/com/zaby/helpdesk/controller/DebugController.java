package com.zaby.helpdesk.controller;

import com.zaby.helpdesk.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import com.zaby.helpdesk.service.TicketService;
import com.zaby.helpdesk.dto.response.TicketResponse;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/debug")
@RequiredArgsConstructor
public class DebugController {

    private final TicketRepository ticketRepository;
    private final TicketService ticketService;

    @GetMapping("/mytickets")
    public Page<TicketResponse> dumpMyTickets() {
        return ticketService.getMyTickets(12L, 0, 10);
    }

    @GetMapping("/test8")
    public Object test8() {
        return ticketRepository.findAll().stream()
                .filter(t -> t.getCreatedBy() != null && t.getCreatedBy() == 8)
                .map(t -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", t.getId());
                    map.put("status", t.getStatus());
                    map.put("deleted", t.isDeleted());
                    return map;
                })
                .collect(Collectors.toList());
    }

    @GetMapping("/tickets")
    public List<Map<String, Object>> dumpTickets() {
        return ticketRepository.findAll().stream().map(t -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", t.getId());
            map.put("title", t.getTicketTitle() != null ? t.getTicketTitle() : "null");
            map.put("status", t.getStatus() != null ? t.getStatus().name() : "null");
            map.put("createdBy", t.getCreatedBy() != null ? t.getCreatedBy() : "null");
            map.put("deleted", t.isDeleted());
            return map;
        }).collect(Collectors.toList());
    }
}
