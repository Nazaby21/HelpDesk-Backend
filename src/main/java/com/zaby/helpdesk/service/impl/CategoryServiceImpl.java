package com.zaby.helpdesk.service.impl;

import com.zaby.helpdesk.dto.request.CategoryRequest;
import com.zaby.helpdesk.dto.response.CategoryResponse;
import com.zaby.helpdesk.mapper.CategoryMapper;
import com.zaby.helpdesk.model.Category;
import com.zaby.helpdesk.repository.CategoryRepository;
import com.zaby.helpdesk.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    public CategoryResponse createCategory(CategoryRequest categoryRequest) {
        Category category = categoryMapper.toEntity(categoryRequest);

        if (categoryRequest.parentId() != null) {
            Category parent = categoryRepository.findById(categoryRequest.parentId())
                    .orElseThrow(() -> new RuntimeException("Parent Category not found"));
            category.setParent(parent);
        }

        Category saveCategory = categoryRepository.save(category);
        return categoryMapper.toCatogoryResponse(saveCategory);
    }

    @Override
    public CategoryResponse updateCategory(Long id, CategoryRequest categoryRequest) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        category.setName(categoryRequest.name());
        category.setDescription(categoryRequest.description());

        if (categoryRequest.parentId() != null) {
            // Prevent circular dependency (a category cannot be its own parent)
            if (id.equals(categoryRequest.parentId())) {
                throw new RuntimeException("A category cannot be its own parent");
            }
            Category parent = categoryRepository.findById(categoryRequest.parentId())
                    .orElseThrow(() -> new RuntimeException("Parent Category not found"));
            category.setParent(parent);
        } else {
            category.setParent(null); // Clear parent if not provided
        }

        categoryRepository.save(category);
        return categoryMapper.toCatogoryResponse(category);
    }

    @Override
    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        return categoryMapper.toCatogoryResponse(category);
    }

    @Override
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAllByParentIsNull()
                .stream()
                .map(categoryMapper::toCatogoryResponse)
                .toList();
    }

    @Override
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        categoryRepository.delete(category);
    }
}
