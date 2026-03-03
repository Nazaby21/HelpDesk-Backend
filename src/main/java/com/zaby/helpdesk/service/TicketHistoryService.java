package com.zaby.helpdesk.service;

import com.zaby.helpdesk.dto.response.TicketHistoryResponse;

import java.util.List;

public interface TicketHistoryService {
    List<TicketHistoryResponse> findByTicketId(Long id);
}
