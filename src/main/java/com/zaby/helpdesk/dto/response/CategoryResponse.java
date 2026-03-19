package com.zaby.helpdesk.dto.response;

public record CategoryResponse(
        Long id,
        String name,
        String description,
        Long parentId,
        java.util.List<CategoryResponse> subCategories
) {
}
