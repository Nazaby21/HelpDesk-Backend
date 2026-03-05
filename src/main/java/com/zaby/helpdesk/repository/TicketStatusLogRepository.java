package com.zaby.helpdesk.repository;

import com.zaby.helpdesk.model.TicketStatusLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketStatusLogRepository extends JpaRepository<TicketStatusLog,Long> {
    List<TicketStatusLog> findByTicketId(Long id);
}
