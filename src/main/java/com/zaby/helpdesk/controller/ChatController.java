package com.zaby.helpdesk.controller;

import com.zaby.helpdesk.model.ChatMessage;
import com.zaby.helpdesk.repository.ChatMessageRepository;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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

    // REST endpoint to fetch all messages for a specific ticket
    @GetMapping("/api/v1/tickets/{ticketId}/messages")
    public List<ChatMessage> getTicketMessages(@PathVariable Long ticketId) {
        return chatMessageRepository.findByTicketIdOrderByTimestampAsc(ticketId);
    }
}
