package com.zaby.helpdesk.repository;

import com.zaby.helpdesk.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket,Long> {
    Optional<Ticket> findById(Long id);
    Optional<Ticket> findByIdAndDeletedFalse(Long id);
    org.springframework.data.domain.Page<Ticket> findByStatusInAndDeletedFalse(java.util.List<com.zaby.helpdesk.enumeration.Status> statuses, org.springframework.data.domain.Pageable pageable);
    org.springframework.data.domain.Page<Ticket> findByCreatedByAndStatusInAndDeletedFalse(Long createdBy, java.util.List<com.zaby.helpdesk.enumeration.Status> statuses, org.springframework.data.domain.Pageable pageable);
}
