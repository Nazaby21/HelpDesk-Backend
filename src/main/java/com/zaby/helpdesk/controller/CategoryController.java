package com.zaby.helpdesk.controller;

import com.zaby.helpdesk.dto.request.CategoryRequest;
import com.zaby.helpdesk.dto.response.CategoryResponse;
import com.zaby.helpdesk.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/categories")
public class CategoryController {
    private final CategoryService categoryService;

    // create department
    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(@RequestBody CategoryRequest categoryRequest) {
        CategoryResponse categoryResponse = categoryService.createCategory(categoryRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryResponse);
    }

    // update department
    @PutMapping
    public ResponseEntity<CategoryResponse> updateCategory(@RequestBody CategoryRequest categoryRequest, @PathVariable Long id) {
        CategoryResponse categoryResponse = categoryService.updateCategory(id, categoryRequest);
        return ResponseEntity.ok(categoryResponse);
    }

    // find all
    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        List<CategoryResponse> categoryResponseList = categoryService.getAllCategories();
        return ResponseEntity.ok(categoryResponseList);
    }

    // find by id
    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable Long id) {
        CategoryResponse categoryResponse = categoryService.getCategoryById(id);
        return ResponseEntity.ok(categoryResponse);
    }
}
