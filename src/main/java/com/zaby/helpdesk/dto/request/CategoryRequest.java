package com.zaby.helpdesk.dto.request;

public record CategoryRequest(
        String name,
        String description,
        Long parentId
) {
}
