package com.example.userservice.feature.classes.controller;

import com.example.dto.ApiResponse;
import com.example.dto.PageDTO;
import com.example.userservice.feature.classes.dto.ClassRequest;
import com.example.userservice.feature.classes.dto.ClassResponse;
import com.example.userservice.feature.classes.service.ClassService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/classes")
@RequiredArgsConstructor
public class ClassController {
    private final ClassService classService;

    @GetMapping("/department/{departmentId}/class-ids")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<Long>> getClassIdsByDepartment(@PathVariable Long departmentId) {
        List<Long> classIds = classService.getClassIdsByDepartment(departmentId);
        return ApiResponse.success(classIds, "Lay danh sach ID lop hoc thanh cong");
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<ClassResponse>> getClasses(
            @RequestParam(required = false) Long majorId,
            @RequestParam(required = false) String academicYear) {
        return ApiResponse.success(classService.getClassesByMajor(majorId, academicYear));
    }

    @GetMapping("/page")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageDTO<ClassResponse>> getClassPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long majorId,
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) Boolean active) {
        return ApiResponse.success(classService.getClassPage(
                page, size, keyword, departmentId, majorId, academicYear, active));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<ClassResponse> getClassById(@PathVariable Long id) {
        return ApiResponse.success(classService.getClassById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ClassResponse> createClass(@RequestBody @Valid ClassRequest request) {
        ClassResponse created = classService.createClass(request);
        return ApiResponse.success(created, "Tao lop hoc thanh cong");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ClassResponse> updateClass(@PathVariable Long id, @RequestBody @Valid ClassRequest request) {
        ClassResponse updated = classService.updateClass(id, request);
        return ApiResponse.success(updated, "Cap nhat lop hoc thanh cong");
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ClassResponse> activateClass(@PathVariable Long id) {
        ClassResponse activated = classService.activateClass(id);
        return ApiResponse.success(activated, "Kich hoat lop hoc thanh cong");
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ClassResponse> deactivateClass(@PathVariable Long id) {
        ClassResponse deactivated = classService.deactivateClass(id);
        return ApiResponse.success(deactivated, "Vo hieu hoa lop hoc thanh cong");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteClass(@PathVariable Long id) {
        classService.deleteClass(id);
        return ApiResponse.of(200, "Xoa lop hoc thanh cong", null);
    }
}
