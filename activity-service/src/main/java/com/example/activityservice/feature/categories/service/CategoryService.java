package com.example.activityservice.feature.categories.service;

import com.example.activityservice.feature.categories.dto.CategoryRequest;
import com.example.activityservice.feature.categories.dto.CategoryResponse;

import java.util.List;

public interface CategoryService {
    CategoryResponse createCategory(CategoryRequest request);

    List<CategoryResponse> getAllCategoriesAsTree(Boolean active);

    List<CategoryResponse> getAllCategoriesFlat(Boolean active, Long parentId);

    CategoryResponse getCategoryById(Long id);

    CategoryResponse updateCategory(Long id, CategoryRequest request);

    CategoryResponse activateCategory(Long id);

    CategoryResponse deactivateCategory(Long id);

    void deleteCategory(Long id);
}
