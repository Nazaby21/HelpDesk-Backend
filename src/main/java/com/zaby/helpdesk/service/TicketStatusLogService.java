package com.zaby.helpdesk.service;

import com.zaby.helpdesk.dto.response.TicketStatusLogResponse;
import com.zaby.helpdesk.model.TicketStatusLog;

import java.util.List;

public interface TicketStatusLogService {
    List<TicketStatusLogResponse> getLogsByTicketId(Long id);
}
