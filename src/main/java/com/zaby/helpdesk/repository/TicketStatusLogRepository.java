package com.zaby.helpdesk.repository;

import com.zaby.helpdesk.model.TicketStatusLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketStatusLogRepository extends JpaRepository<TicketStatusLog,Long> {
    List<TicketStatusLog> findByTicketId(Long id);
}
