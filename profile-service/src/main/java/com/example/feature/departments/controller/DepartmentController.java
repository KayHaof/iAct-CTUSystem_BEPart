package com.example.feature.departments.controller;

import com.example.dto.ApiResponse;
import com.example.dto.PageDTO;
import com.example.feature.departments.dto.DepartmentRequest;
import com.example.feature.departments.dto.DepartmentResponse;
import com.example.feature.departments.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
public class DepartmentController {
    private final DepartmentService departmentService;

    @PostMapping
    @PreAuthorize("hasAuthority('admin') or hasRole('ADMIN')")
    public ApiResponse<DepartmentResponse> create(@RequestBody DepartmentRequest request) {
        return ApiResponse.<DepartmentResponse>builder()
                .result(departmentService.createDepartment(request))
                .message("Tạo khoa/phòng ban thành công")
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('admin') or hasRole('ADMIN')")
    public ApiResponse<DepartmentResponse> update(
            @PathVariable Long id,
            @RequestBody DepartmentRequest request) {
        return ApiResponse.<DepartmentResponse>builder()
                .result(departmentService.updateDepartment(id, request))
                .message("Cập nhật khoa/phòng ban thành công")
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('admin') or hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        departmentService.deleteDepartment(id);
        return ApiResponse.<Void>builder()
                .message("Xóa khoa/phòng ban thành công")
                .build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<DepartmentResponse> getById(@PathVariable Long id) {
        return ApiResponse.<DepartmentResponse>builder()
                .result(departmentService.getDepartmentById(id))
                .build();
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageDTO<DepartmentResponse>> getAll(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {

        return ApiResponse.<PageDTO<DepartmentResponse>>builder()
                .result(departmentService.getDepartments(page, size, keyword))
                .build();
    }
}