package com.zaby.helpdesk.mapper;

import com.zaby.helpdesk.dto.request.TicketStatusLogRequest;
import com.zaby.helpdesk.dto.response.TicketStatusLogResponse;
import com.zaby.helpdesk.model.TicketStatusLog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TicketStatusLogMapper {

    @Mapping(target = "ticketId", source = "ticket.id")
    @Mapping(target = "changedByName",
            expression = "java(ticketStatusLog.getChangedBy() != null ? ticketStatusLog.getChangedBy().getFirstName() + \" \" + ticketStatusLog.getChangedBy().getLastName() : null)")

    TicketStatusLogResponse toResponse(TicketStatusLog ticketStatusLog);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ticket", ignore = true)
    @Mapping(target = "fromStatus", ignore = true)
    @Mapping(target = "changedBy", ignore = true)
    @Mapping(target = "changedAt", ignore = true)
    TicketStatusLog toEntity(TicketStatusLogRequest ticketStatusLogRequest);
}
