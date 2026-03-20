package com.zaby.helpdesk.dto.response;

public record DashboardStatsResponse(
        long totalTickets,
        long pendingTickets,
        long inProgressTickets,
        long completedTickets,
        long totalUsers
) {}
