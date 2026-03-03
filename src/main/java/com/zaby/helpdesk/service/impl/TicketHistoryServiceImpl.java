package com.zaby.helpdesk.service.impl;

import com.zaby.helpdesk.dto.response.TicketHistoryResponse;
import com.zaby.helpdesk.mapper.TicketHistoryMapper;
import com.zaby.helpdesk.repository.TicketHistoryRepository;
import com.zaby.helpdesk.service.TicketHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketHistoryServiceImpl implements TicketHistoryService {

    private final TicketHistoryRepository ticketHistoryRepository;
    private final TicketHistoryMapper ticketHistoryMapper;

    @Override
    public List<TicketHistoryResponse> findByTicketId(Long ticketId){
        return ticketHistoryRepository.findByTicketId(ticketId)
                .stream()
                .map(ticketHistoryMapper::toResponse)
                .toList();
    }
}
