package com.zaby.helpdesk.mapper;

import com.zaby.helpdesk.dto.response.TicketHistoryResponse;
import com.zaby.helpdesk.model.TicketHistory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TicketHistoryMapper {

    @Mapping(source = "ticket.id", target = "ticketId")
    @Mapping(source = "performedBy.id", target = "performById")
    @Mapping(target = "performByName",
            expression = "java(ticketHistory.getPerformedBy() != null ? ticketHistory.getPerformedBy().getFirstName() + \" \" + ticketHistory.getPerformedBy().getLastName() : null)")
    TicketHistoryResponse toResponse(TicketHistory ticketHistory);
}
