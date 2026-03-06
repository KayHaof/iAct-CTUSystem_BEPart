package com.example.feature.categories.controller;

import com.example.dto.ApiResponse;
import com.example.feature.categories.dto.CategoryRequest;
import com.example.feature.categories.dto.CategoryResponse;
import com.example.feature.categories.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    // Tạo danh mục (Chỉ Admin)
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(@RequestBody CategoryRequest request) {
        return new ResponseEntity<>(
                ApiResponse.success(categoryService.createCategory(request)),
                HttpStatus.CREATED
        );
    }

    // Lấy danh sách dạng Cây lồng nhau (Tree)
    @GetMapping("/tree")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategoriesTree() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getAllCategoriesAsTree()));
    }

    // Lấy danh sách dạng Phẳng (Flat) - Dành cho dropdown list đơn giản
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategoriesFlat() {
        System.out.println(">> Called fetch all category");
        return ResponseEntity.ok(ApiResponse.success(categoryService.getAllCategoriesFlat()));
    }

    // Xem chi tiết 1 danh mục
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getCategoryById(id)));
    }

    // Sửa danh mục (Chỉ Admin)
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable Long id,
            @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.updateCategory(id, request)));
    }

    // Xóa danh mục (Chỉ Admin)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
