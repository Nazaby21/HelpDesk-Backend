package com.zaby.helpdesk.controller;

import com.zaby.helpdesk.model.ChatMessage;
import com.zaby.helpdesk.repository.ChatMessageRepository;
import com.zaby.helpdesk.security.CustomUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
public class ChatController {

    private final ChatMessageRepository chatMessageRepository;
    private final SimpMessagingTemplate simpMessagingTemplate;

    public ChatController(ChatMessageRepository chatMessageRepository, SimpMessagingTemplate simpMessagingTemplate) {
        this.chatMessageRepository = chatMessageRepository;
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    // WebSocket endpoint
    @MessageMapping("/chat")
    public void sendMessage(ChatMessage message) {
        message.setTimestamp(LocalDateTime.now());
        chatMessageRepository.save(message);
        simpMessagingTemplate.convertAndSend("/topic/ticket/" + message.getTicketId(), message);
    }

    // REST endpoint to post a message to a specific ticket (persists and broadcasts over STOMP)
    @PostMapping("/api/v1/tickets/{ticketId}/messages")
    public ResponseEntity<ChatMessage> postTicketMessage(
            @PathVariable Long ticketId,
            @RequestBody ChatMessage message,
            Authentication authentication
    ) {
        message.setTicketId(ticketId);
        if ((message.getSender() == null || message.getSender().isBlank()) && authentication != null && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            String fullName = (userDetails.getUser().getFirstName() + " " + userDetails.getUser().getLastName()).trim();
            message.setSender(fullName.isEmpty() ? userDetails.getUsername() : fullName);
        }
        message.setTimestamp(LocalDateTime.now());
        ChatMessage saved = chatMessageRepository.save(message);
        simpMessagingTemplate.convertAndSend("/topic/ticket/" + ticketId, saved);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // REST endpoint to fetch messages for a specific ticket (supports pagination if page or size is provided, else returns List for web client)
    @GetMapping("/api/v1/tickets/{ticketId}/messages")
    public ResponseEntity<?> getTicketMessages(
            @PathVariable Long ticketId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        if (page != null || size != null) {
            int pageNum = page != null ? page : 0;
            int pageSize = size != null ? size : 20;
            Page<ChatMessage> messages = chatMessageRepository.findByTicketIdOrderByTimestampAsc(ticketId, PageRequest.of(pageNum, pageSize));
            return ResponseEntity.ok(messages);
        }
        List<ChatMessage> messages = chatMessageRepository.findByTicketIdOrderByTimestampAsc(ticketId);
        return ResponseEntity.ok(messages);
    }
}
