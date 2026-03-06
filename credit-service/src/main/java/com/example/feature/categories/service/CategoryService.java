package com.example.feature.categories.service;

import com.example.feature.categories.dto.CategoryRequest;
import com.example.feature.categories.dto.CategoryResponse;

import java.util.List;

public interface CategoryService {
    CategoryResponse createCategory(CategoryRequest request);
    List<CategoryResponse> getAllCategoriesAsTree();
    List<CategoryResponse> getAllCategoriesFlat();
    CategoryResponse getCategoryById(Long id);
    CategoryResponse updateCategory(Long id, CategoryRequest request);
    void deleteCategory(Long id);
}
