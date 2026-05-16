package com.example.userservice.feature.departments.controller;

import com.example.dto.ApiResponse;
import com.example.dto.PageDTO;
import com.example.userservice.feature.departments.dto.DepartmentRequest;
import com.example.userservice.feature.departments.dto.DepartmentResponse;
import com.example.userservice.feature.departments.service.DepartmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
public class DepartmentController {
    private final DepartmentService departmentService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<DepartmentResponse> create(@RequestBody @Valid DepartmentRequest request) {
        return ApiResponse.success(departmentService.createDepartment(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<DepartmentResponse> update(
            @PathVariable Long id,
            @RequestBody @Valid DepartmentRequest request) {
        return ApiResponse.success(departmentService.updateDepartment(id, request));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<DepartmentResponse> activate(@PathVariable Long id) {
        return ApiResponse.success(departmentService.activateDepartment(id));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<DepartmentResponse> deactivate(@PathVariable Long id) {
        return ApiResponse.success(departmentService.deactivateDepartment(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        departmentService.deleteDepartment(id);
        return ApiResponse.success(null);
    }

    @GetMapping("/options")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<DepartmentResponse>> getOptions(@RequestParam(required = false) Boolean active) {
        return ApiResponse.success(departmentService.getDepartmentOptions(active));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<DepartmentResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(departmentService.getDepartmentById(id));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageDTO<DepartmentResponse>> getAll(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean active) {
        return ApiResponse.success(departmentService.getDepartments(page, size, keyword, active));
    }
}
