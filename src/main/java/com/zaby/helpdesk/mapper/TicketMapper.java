package com.zaby.helpdesk.mapper;

import com.zaby.helpdesk.dto.request.TicketRequest;
import com.zaby.helpdesk.dto.response.TicketResponse;
import com.zaby.helpdesk.model.Ticket;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {TicketStatusLogMapper.class})
public interface TicketMapper {

    @Mapping(source = "category.id", target = "categoryId")
    @Mapping(source = "category.name", target = "categoryName")
    @Mapping(target = "subCategoryId", expression = "java(ticket.getSubCategory() != null ? ticket.getSubCategory().getId() : null)")
    @Mapping(target = "subCategoryName", expression = "java(ticket.getSubCategory() != null ? ticket.getSubCategory().getName() : null)")
    @Mapping(target = "assignedTo", expression = "java(ticket.getAssignedTo() != null ? ticket.getAssignedTo().getId() : null)")
    @Mapping(target = "assignedName", expression = "java(ticket.getAssignedTo() != null ? ticket.getAssignedTo().getFirstName() + \" \" + ticket.getAssignedTo().getLastName() : null)")
    @Mapping(source = "ticketStatusLog", target = "logs")
    @Mapping(target = "createdAt", expression = "java(ticket.getCreatedAt())")
    @Mapping(target = "createdByName", ignore = true)
    @Mapping(target = "completedAt", ignore = true)
    TicketResponse toTicketResponse(Ticket ticket);

    @Mapping(target = "category", ignore = true)
    @Mapping(target = "assignedTo", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    Ticket toEntity(TicketRequest request);
}
