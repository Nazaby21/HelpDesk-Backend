package com.zaby.helpdesk.repository;

import com.zaby.helpdesk.model.TicketHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketHistoryRepository extends JpaRepository<TicketHistory, Long> {
    List<TicketHistory> findByTicketId(Long id);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"ticket", "performedBy"})
    org.springframework.data.domain.Page<TicketHistory> findByTicketCreatedByOrderByPerformAtDesc(Long createdBy, org.springframework.data.domain.Pageable pageable);
    
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"ticket", "performedBy"})
    org.springframework.data.domain.Page<TicketHistory> findByTicketAssignedToIdOrderByPerformAtDesc(Long assignedToId, org.springframework.data.domain.Pageable pageable);
    
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"ticket", "performedBy"})
    org.springframework.data.domain.Page<TicketHistory> findAllByOrderByPerformAtDesc(org.springframework.data.domain.Pageable pageable);
}
