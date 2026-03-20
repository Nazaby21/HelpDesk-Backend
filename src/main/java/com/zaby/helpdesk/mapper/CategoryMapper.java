package com.zaby.helpdesk.mapper;

import com.zaby.helpdesk.dto.request.CategoryRequest;
import com.zaby.helpdesk.dto.response.CategoryResponse;
import com.zaby.helpdesk.model.Category;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    CategoryResponse toCatogoryResponse(Category category);

    Category toEntity(CategoryRequest request);
}
