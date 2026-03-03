package com.zaby.helpdesk.service.impl;

import com.zaby.helpdesk.dto.response.TicketStatusLogResponse;
import com.zaby.helpdesk.mapper.TicketStatusLogMapper;
import com.zaby.helpdesk.model.TicketStatusLog;
import com.zaby.helpdesk.repository.TicketStatusLogRepository;
import com.zaby.helpdesk.service.TicketStatusLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketStatusLogServiceImpl implements TicketStatusLogService {
    private TicketStatusLogRepository ticketStatusLogRepository;
    private TicketStatusLogMapper ticketStatusLogMapper;

    @Override
    public List<TicketStatusLogResponse> getLogsByTicketId(Long ticketId){
        return ticketStatusLogRepository.findByTicketId(ticketId)
                .stream()
                .map(ticketStatusLogMapper::toResponse)
                .toList();
    }
}
