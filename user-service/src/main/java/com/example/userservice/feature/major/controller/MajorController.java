package com.example.userservice.feature.major.controller;

import com.example.dto.ApiResponse;
import com.example.dto.PageDTO;
import com.example.userservice.feature.major.dto.MajorRequest;
import com.example.userservice.feature.major.dto.MajorResponse;
import com.example.userservice.feature.major.service.MajorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/majors")
@RequiredArgsConstructor
public class MajorController {

    private final MajorService majorService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<MajorResponse>> getMajors(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Boolean active) {
        return ApiResponse.success(majorService.getMajors(departmentId, active));
    }

    @GetMapping("/page")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageDTO<MajorResponse>> getMajorPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String programType) {
        return ApiResponse.success(majorService.getMajorPage(page, size, keyword, departmentId, active, programType));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<MajorResponse> getMajorById(@PathVariable Long id) {
        return ApiResponse.success(majorService.getMajorById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<MajorResponse> createMajor(@RequestBody @Valid MajorRequest request) {
        return ApiResponse.success(majorService.createMajor(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<MajorResponse> updateMajor(@PathVariable Long id, @RequestBody @Valid MajorRequest request) {
        return ApiResponse.success(majorService.updateMajor(id, request));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<MajorResponse> activateMajor(@PathVariable Long id) {
        return ApiResponse.success(majorService.activateMajor(id));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<MajorResponse> deactivateMajor(@PathVariable Long id) {
        return ApiResponse.success(majorService.deactivateMajor(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteMajor(@PathVariable Long id) {
        majorService.deleteMajor(id);
        return ApiResponse.success(null);
    }
}
