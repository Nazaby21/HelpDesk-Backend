package com.zaby.helpdesk.service.impl;

import com.zaby.helpdesk.dto.request.TicketRequest;
import com.zaby.helpdesk.dto.response.DashboardStatsResponse;
import com.zaby.helpdesk.dto.response.MonthlyChartResponse;
import com.zaby.helpdesk.dto.response.NotificationResponse;
import com.zaby.helpdesk.dto.response.TicketResponse;
import com.zaby.helpdesk.enumeration.Action;
import com.zaby.helpdesk.enumeration.Priority;
import com.zaby.helpdesk.enumeration.Status;
import com.zaby.helpdesk.exception.ResourceNotFoundException;
import com.zaby.helpdesk.mapper.TicketMapper;
import com.zaby.helpdesk.model.*;
import com.zaby.helpdesk.repository.CategoryRepository;
import com.zaby.helpdesk.repository.TicketHistoryRepository;
import com.zaby.helpdesk.repository.TicketRepository;
import com.zaby.helpdesk.repository.UserRepository;
import com.zaby.helpdesk.service.TicketService;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class TicketServiceImpl implements TicketService {

    private final TicketMapper ticketMapper;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final TicketHistoryRepository ticketHistoryRepository;
    private final CategoryRepository categoryRepository;

    private static final Map<Status, List<Status>> allowedTransitions = Map.of(
            Status.PENDING, List.of(Status.IN_PROGRESS),
            Status.IN_PROGRESS, List.of(Status.COMPLETED),
            Status.COMPLETED, List.of()
    );

    private void validateStatusTransition(Status from, Status to) {
        List<Status> allowed = allowedTransitions.get(from);
        if (allowed == null || !allowed.contains(to)) {
            throw new IllegalArgumentException("Invalid transition from " + from + " to " + to + ". Allowed: " + allowed);
        }
    }

    private TicketResponse enrichResponse(Ticket ticket, TicketResponse response) {
        // Resolve creator name
        String createdByName = null;
        if (ticket.getCreatedBy() != null) {
            createdByName = userRepository.findById(ticket.getCreatedBy())
                    .map(u -> u.getFirstName() + " " + u.getLastName())
                    .orElse("Unknown");
        }

        // Derive completedAt from status logs
        LocalDateTime completedAt = ticket.getTicketStatusLog().stream()
                .filter(log -> "COMPLETED".equals(log.getToStatus()))
                .map(TicketStatusLog::getChangedAt)
                .reduce((first, second) -> second) // get the latest one
                .orElse(null);

        return new TicketResponse(
                response.id(),
                response.ticketTitle(),
                response.description(),
                response.priority(),
                response.status(),
                response.categoryId(),
                response.categoryName(),
                response.subCategoryId(),
                response.subCategoryName(),
                response.assignedTo(),
                response.assignedName(),
                response.logs(),
                response.createdBy(),
                createdByName,
                response.createdAt(),
                completedAt,
                response.imageUrls()
        );
    }

    @Override
    @Transactional
    public TicketResponse createTicket(TicketRequest ticketRequest) {
        // 1. Convert Request DTO -> Entity
        Ticket ticket = ticketMapper.toEntity(ticketRequest);

        // status
        ticket.setStatus(Status.PENDING);

        // set category
        Category category = categoryRepository.findById(ticketRequest.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        ticket.setCategory(category);

        // set sub category
        if (ticketRequest.subCategoryId() != null) {
            Category subCategory = categoryRepository.findById(ticketRequest.subCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("SubCategory not found"));
            ticket.setSubCategory(subCategory);
        }

        // set assigned user
        if (ticketRequest.assignedTo() != null) {
            User assignedUser = userRepository.findById(ticketRequest.assignedTo())
                    .orElseThrow(() -> new ResourceNotFoundException("User Not Found"));
            ticket.setAssignedTo(assignedUser);
        }

        // create initial StatusLog
        TicketStatusLog ticketStatusLog = new TicketStatusLog();
        ticketStatusLog.setTicket(ticket);
        ticketStatusLog.setFromStatus(null);
        ticketStatusLog.setToStatus(Status.PENDING.name());
        ticketStatusLog.setRemark("Ticket Created");
        ticketStatusLog.setChangedAt(LocalDateTime.now());

        ticket.getTicketStatusLog().add(ticketStatusLog); // add to ticket

        // create initial history
        TicketHistory ticketHistory = new TicketHistory();
        ticketHistory.setTicket(ticket);
        ticketHistory.setAction(Action.CREATE);
        ticketHistory.setComment("Ticket Created");
        ticketHistory.setPerformAt(LocalDateTime.now());

        ticketHistoryRepository.save(ticketHistory);

        Ticket savedTicket = ticketRepository.save(ticket); // save ticket
        return enrichResponse(savedTicket, ticketMapper.toTicketResponse(savedTicket));
    }

    @Override
    @Transactional
    public TicketResponse updateTicket(Long id, TicketRequest ticketRequest) {

        // check ticket
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket Not Found"));

        if (ticket.isDeleted()) {
            throw new IllegalStateException("Deleted ticket cannot be updated");
        }

        // validate ticket after status change to complete
        if(ticket.getStatus() == Status.COMPLETED) {
            throw new IllegalArgumentException("Completed ticket cannot be modified");
        }

        // update ticket
        ticket.setTicketTitle(ticketRequest.ticketTitle());
        ticket.setDescription(ticketRequest.description());
        ticket.setPriority(ticketRequest.priority());

        // Update category if provided
        if (ticketRequest.categoryId() != null) {
            Category category = categoryRepository.findById(ticketRequest.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category Not Found"));
            ticket.setCategory(category);
        }

        // Update sub category if provided
        if (ticketRequest.subCategoryId() != null) {
            Category subCategory = categoryRepository.findById(ticketRequest.subCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("SubCategory Not Found"));
            ticket.setSubCategory(subCategory);
        }

        // Update assigned user if provided
        if (ticketRequest.assignedTo() != null) {
            User assignedUser = userRepository.findById(ticketRequest.assignedTo())
                    .orElseThrow(() -> new ResourceNotFoundException("User Not Found"));
            ticket.setAssignedTo(assignedUser);
        }

        // add history
        TicketHistory ticketHistory = new TicketHistory();
        ticketHistory.setTicket(ticket);
        ticketHistory.setAction(Action.UPDATE);
        ticketHistory.setComment("Ticket information updated");
        ticketHistory.setPerformAt(LocalDateTime.now());

        ticket.getTicketHistory().add(ticketHistory);

        Ticket savedTicket = ticketRepository.save(ticket);
        return enrichResponse(savedTicket, ticketMapper.toTicketResponse(savedTicket));
    }

    @Override
    @Transactional
    public TicketResponse updateTicketStatus(Long id, Status status, String remark, Long assignedUserId) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket Not Found"));

        Status currentStatus = ticket.getStatus();

        if (ticket.isDeleted()) {
            throw new IllegalStateException("Deleted ticket cannot be updated");
        }

        // cant update if it completed
        if(currentStatus == Status.COMPLETED) {
            throw new IllegalArgumentException("Completed ticket cannot be modified");
        }

        // validate status
        validateStatusTransition(currentStatus, status);

        // update status
        ticket.setStatus(status);

        // auto-assign technician when accepting (PENDING -> IN_PROGRESS)
        if (status == Status.IN_PROGRESS && ticket.getAssignedTo() == null && assignedUserId != null) {
            User technician = userRepository.findById(assignedUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("Technician Not Found"));
            ticket.setAssignedTo(technician);
        }

        // create status log
        TicketStatusLog log = new TicketStatusLog();
        log.setTicket(ticket);
        log.setFromStatus(currentStatus.name());
        log.setToStatus(status.name());
        log.setRemark(remark);
        log.setChangedAt(LocalDateTime.now());

        ticket.getTicketStatusLog().add(log);

        // add history
        TicketHistory history = new TicketHistory();
        history.setTicket(ticket);
        history.setAction(Action.UPDATE);
        history.setComment("Status changed from " + currentStatus + " to " + status);
        history.setPerformAt(LocalDateTime.now());

        ticket.getTicketHistory().add(history);
        Ticket savedTicket = ticketRepository.save(ticket);

        return enrichResponse(savedTicket, ticketMapper.toTicketResponse(savedTicket));
    }

    @Override
    public TicketResponse deletedTicket(Long id) {
        Ticket ticket = ticketRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket Not Found"));

        if (ticket.getStatus() == Status.COMPLETED) {
            throw new IllegalArgumentException("Completed ticket cannot be deleted");
        }

        ticket.setDeleted(true);

        // add history
        TicketHistory ticketHistory = new TicketHistory();
        ticketHistory.setTicket(ticket);
        ticketHistory.setAction(Action.DELETE);
        ticketHistory.setComment("Ticket Deleted");
        ticketHistory.setPerformAt(LocalDateTime.now());

        ticket.getTicketHistory().add(ticketHistory);

        Ticket savedTicket = ticketRepository.save(ticket);
        return enrichResponse(savedTicket, ticketMapper.toTicketResponse(savedTicket));
    }

    @Override
    public TicketResponse getById(Long id) {
        Ticket ticket = ticketRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket Not Found"));

        return enrichResponse(ticket, ticketMapper.toTicketResponse(ticket));
    }

    private Specification<Ticket> buildSpecification(
            List<Status> defaultStatuses,
            Status status,
            Priority priority,
            String search,
            Long createdBy,
            Long assignedTo
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("deleted")));

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            } else if (defaultStatuses != null && !defaultStatuses.isEmpty()) {
                predicates.add(root.get("status").in(defaultStatuses));
            }

            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }

            if (search != null && !search.trim().isEmpty()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("ticketTitle")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)
                ));
            }

            if (createdBy != null) {
                predicates.add(cb.equal(root.get("createdBy"), createdBy));
            }

            if (assignedTo != null) {
                Join<Ticket, User> assigned = root.join("assignedTo", JoinType.LEFT);
                predicates.add(cb.equal(assigned.get("id"), assignedTo));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Override
    public Page<TicketResponse> getAll(int page, int size, Status status, Priority priority, String search) {
        Pageable pageable = PageRequest.of(page, size);
        Specification<Ticket> spec = buildSpecification(
                List.of(Status.PENDING, Status.IN_PROGRESS),
                status, priority, search, null, null
        );
        Page<Ticket> tickets = ticketRepository.findAll(spec, pageable);
        return tickets.map(t -> enrichResponse(t, ticketMapper.toTicketResponse(t)));
    }

    @Override
    public Page<TicketResponse> getHistory(Long userId, String role, int page, int size, Status status, Priority priority, String search) {
        Pageable pageable = PageRequest.of(page, size);
        Long createdBy = "ROLE_USER".equals(role) ? userId : null;
        Long assignedTo = "ROLE_TECHNICIAN".equals(role) ? userId : null;

        Specification<Ticket> spec = buildSpecification(
                List.of(Status.COMPLETED),
                status, priority, search, createdBy, assignedTo
        );
        Page<Ticket> tickets = ticketRepository.findAll(spec, pageable);
        return tickets.map(t -> enrichResponse(t, ticketMapper.toTicketResponse(t)));
    }

    @Override
    public Page<TicketResponse> getMyTickets(Long userId, int page, int size, Status status, Priority priority, String search) {
        Pageable pageable = PageRequest.of(page, size);
        Specification<Ticket> spec = buildSpecification(
                List.of(Status.PENDING, Status.IN_PROGRESS),
                status, priority, search, userId, null
        );
        Page<Ticket> tickets = ticketRepository.findAll(spec, pageable);
        return tickets.map(t -> enrichResponse(t, ticketMapper.toTicketResponse(t)));
    }

    @Override
    public DashboardStatsResponse getDashboardStats() {
        long total = ticketRepository.countByDeletedFalse();
        long pending = ticketRepository.countByStatusAndDeletedFalse(Status.PENDING);
        long inProgress = ticketRepository.countByStatusAndDeletedFalse(Status.IN_PROGRESS);
        long completed = ticketRepository.countByStatusAndDeletedFalse(Status.COMPLETED);
        long totalUsers = userRepository.count();

        return new DashboardStatsResponse(
                total, pending, inProgress, completed, totalUsers
        );
    }

    @Override
    public MonthlyChartResponse getMonthlyStats(int year) {
        List<Object[]> results = ticketRepository.countTicketsByMonthAndStatus(year);
        long[] received = new long[12];
        long[] due = new long[12];

        for (Object[] row : results) {
            int month = ((Number) row[0]).intValue();
            Status status = (Status) row[1];
            long count = ((Number) row[2]).longValue();

            if (month >= 1 && month <= 12) {
                if (status == Status.COMPLETED) {
                    received[month - 1] += count;
                } else if (status == Status.PENDING || status == Status.IN_PROGRESS) {
                    due[month - 1] += count;
                }
            }
        }

        return new MonthlyChartResponse(
                Arrays.stream(received).boxed().toList(),
                Arrays.stream(due).boxed().toList()
        );
    }

    @Override
    public Page<NotificationResponse> getActivityFeed(Long userId, String role, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<TicketHistory> history;

        if ("ROLE_USER".equals(role)) {
            history = ticketHistoryRepository.findByTicketCreatedByOrderByPerformAtDesc(userId, pageable);
        } else if ("ROLE_TECHNICIAN".equals(role)) {
            history = ticketHistoryRepository.findByTicketAssignedToIdOrderByPerformAtDesc(userId, pageable);
        } else {
            history = ticketHistoryRepository.findAllByOrderByPerformAtDesc(pageable);
        }

        return history.map(h -> {
            String title = "Ticket " + h.getAction();
            String subTitle = "Ticket #" + h.getTicket().getId() + ": " + h.getTicket().getTicketTitle();
            String image = h.getPerformedBy() != null ? h.getPerformedBy().getImageUrl() : "/images/user/user-01.png";
            if (image == null) {
                image = "/images/user/user-01.png";
            }
            return new NotificationResponse(
                    h.getId(),
                    image,
                    title,
                    subTitle,
                    h.getPerformAt()
            );
        });
    }
}
