package com.zaby.helpdesk.repository;

import com.zaby.helpdesk.enumeration.Status;
import com.zaby.helpdesk.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket,Long> {
    Optional<Ticket> findById(Long id);
    Optional<Ticket> findByIdAndDeletedFalse(Long id);
    org.springframework.data.domain.Page<Ticket> findByStatusInAndDeletedFalse(java.util.List<Status> statuses, org.springframework.data.domain.Pageable pageable);
    org.springframework.data.domain.Page<Ticket> findByCreatedByAndStatusInAndDeletedFalse(Long createdBy, java.util.List<Status> statuses, org.springframework.data.domain.Pageable pageable);
    org.springframework.data.domain.Page<Ticket> findByAssignedToIdAndStatusInAndDeletedFalse(Long assignedToId, java.util.List<Status> statuses, org.springframework.data.domain.Pageable pageable);

    // Dashboard stats counts
    long countByDeletedFalse();
    long countByStatusAndDeletedFalse(Status status);

    // Profile stats counts
    long countByCreatedByAndDeletedFalse(Long createdBy);
    long countByAssignedToIdAndDeletedFalse(Long assignedToId);
    long countByCreatedByAndStatusAndDeletedFalse(Long createdBy, Status status);
    long countByAssignedToIdAndStatusAndDeletedFalse(Long assignedToId, Status status);
    long countByCreatedByAndStatusInAndDeletedFalse(Long createdBy, java.util.List<Status> statuses);
    long countByAssignedToIdAndStatusInAndDeletedFalse(Long assignedToId, java.util.List<Status> statuses);

    @org.springframework.data.jpa.repository.Query("SELECT EXTRACT(MONTH FROM t.createdAt), t.status, COUNT(t.id) " +
           "FROM Ticket t " +
           "WHERE EXTRACT(YEAR FROM t.createdAt) = :year AND t.deleted = false " +
           "GROUP BY EXTRACT(MONTH FROM t.createdAt), t.status")
    java.util.List<Object[]> countTicketsByMonthAndStatus(@org.springframework.data.repository.query.Param("year") int year);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(t) FROM Ticket t " +
           "LEFT JOIN t.category c " +
           "LEFT JOIN t.subCategory sc " +
           "LEFT JOIN c.parent cp " +
           "LEFT JOIN sc.parent scp " +
           "WHERE c.id = :categoryId OR sc.id = :categoryId " +
           "OR cp.id = :categoryId OR scp.id = :categoryId")
    long countTicketsByCategoryId(@org.springframework.data.repository.query.Param("categoryId") Long categoryId);
}
