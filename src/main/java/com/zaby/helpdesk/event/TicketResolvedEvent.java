package com.zaby.helpdesk.event;

public record TicketResolvedEvent(
        Long ticketId,
        String ticketTitle,
        Long creatorId,
        Long resolvedByUserId,
        String resolvedByName,
        String resolutionNote
) {
}
