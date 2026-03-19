package com.zaby.helpdesk.repository;

import com.zaby.helpdesk.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByTicketIdOrderByTimestampAsc(Long ticketId);
}
