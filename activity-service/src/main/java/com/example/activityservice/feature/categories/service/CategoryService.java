package com.example.activityservice.feature.categories.service;

import com.example.activityservice.feature.categories.dto.CategoryRequest;
import com.example.activityservice.feature.categories.dto.CategoryResponse;

import java.util.List;

public interface CategoryService {
    CategoryResponse createCategory(CategoryRequest request);
    List<CategoryResponse> getAllCategoriesAsTree();
    List<CategoryResponse> getAllCategoriesFlat();
    CategoryResponse getCategoryById(Long id);
    CategoryResponse updateCategory(Long id, CategoryRequest request);
    void deleteCategory(Long id);
}
