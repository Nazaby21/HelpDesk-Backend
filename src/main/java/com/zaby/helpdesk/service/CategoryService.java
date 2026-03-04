package com.zaby.helpdesk.service;

import com.zaby.helpdesk.dto.request.CategoryRequest;
import com.zaby.helpdesk.dto.response.CategoryResponse;

import java.util.List;

public interface CategoryService {
    CategoryResponse createCategory(CategoryRequest categoryRequest);
    CategoryResponse updateCategory(Long id, CategoryRequest categoryRequest);
    CategoryResponse getCategoryById(Long id);
    List<CategoryResponse> getAllCategories();
}
