package com.example.userservice.feature.classes.controller;

import com.example.dto.ApiResponse;
import com.example.userservice.feature.classes.dto.ClassRepresentativeRequest;
import com.example.userservice.feature.classes.dto.RepresentativeActivityPermissionResponse;
import com.example.userservice.feature.classes.service.ClassRepresentativeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/class-representatives")
@RequiredArgsConstructor
public class ClassRepresentativeController {

    private final ClassRepresentativeService representativeService;

    @GetMapping("/me/activity-permission")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<RepresentativeActivityPermissionResponse> getMyActivityPermission() {
        return ApiResponse.success(representativeService.getCurrentStudentActivityPermission());
    }

    @GetMapping
    @PreAuthorize("hasRole('DEPARTMENT')")
    public ApiResponse<List<RepresentativeActivityPermissionResponse>> getRepresentatives(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(
                representativeService.getRepresentatives(departmentId, classId, active, keyword));
    }

    @PostMapping
    @PreAuthorize("hasRole('DEPARTMENT')")
    public ApiResponse<RepresentativeActivityPermissionResponse> createRepresentative(
            @RequestBody ClassRepresentativeRequest request) {
        return ApiResponse.success(representativeService.createRepresentative(request));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('DEPARTMENT')")
    public ApiResponse<RepresentativeActivityPermissionResponse> deactivateRepresentative(@PathVariable Long id) {
        return ApiResponse.success(representativeService.deactivateRepresentative(id));
    }
}
