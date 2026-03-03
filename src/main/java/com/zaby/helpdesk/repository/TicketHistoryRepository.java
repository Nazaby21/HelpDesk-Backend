package com.zaby.helpdesk.repository;

import com.zaby.helpdesk.model.TicketHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketHistoryRepository extends JpaRepository<TicketHistory, Long> {
    List<TicketHistory> findByTicketId(Long id);
}
