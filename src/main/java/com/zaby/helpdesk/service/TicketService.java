package com.zaby.helpdesk.service;

import com.zaby.helpdesk.dto.request.TicketRequest;
import com.zaby.helpdesk.dto.response.DashboardStatsResponse;
import com.zaby.helpdesk.dto.response.MonthlyChartResponse;
import com.zaby.helpdesk.dto.response.NotificationResponse;
import com.zaby.helpdesk.dto.response.TicketResponse;
import com.zaby.helpdesk.enumeration.Priority;
import com.zaby.helpdesk.enumeration.Status;
import org.springframework.data.domain.Page;

public interface TicketService {
    TicketResponse createTicket(TicketRequest ticketRequest);
    TicketResponse updateTicket(Long id, TicketRequest ticketRequest);
    TicketResponse updateTicketStatus(Long id, Status status, String remark, Long assignedUserId);
    TicketResponse deletedTicket(Long id);
    TicketResponse getById(Long id);

    Page<TicketResponse> getAll(int page, int size, Status status, Priority priority, String search);
    Page<TicketResponse> getHistory(Long userId, String role, int page, int size, Status status, Priority priority, String search);
    Page<TicketResponse> getMyTickets(Long userId, int page, int size, Status status, Priority priority, String search);

    default Page<TicketResponse> getAll(int page, int size) {
        return getAll(page, size, null, null, null);
    }
    default Page<TicketResponse> getHistory(Long userId, String role, int page, int size) {
        return getHistory(userId, role, page, size, null, null, null);
    }
    default Page<TicketResponse> getMyTickets(Long userId, int page, int size) {
        return getMyTickets(userId, page, size, null, null, null);
    }

    DashboardStatsResponse getDashboardStats();
    MonthlyChartResponse getMonthlyStats(int year);
    Page<NotificationResponse> getActivityFeed(Long userId, String role, int page, int size);
}
