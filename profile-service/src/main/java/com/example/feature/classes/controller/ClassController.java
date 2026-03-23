package com.example.feature.classes.controller;

import com.example.dto.ApiResponse;
import com.example.feature.classes.dto.ClassRequest;
import com.example.feature.classes.dto.ClassResponse;
import com.example.feature.classes.service.ClassService;
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
    public ApiResponse<List<Long>> getClassIdsByDepartment(@PathVariable Long departmentId) {
        List<Long> classIds = classService.getClassIdsByDepartment(departmentId);
        return ApiResponse.<List<Long>>builder()
                .result(classIds)
                .message("Lấy danh sách ID lớp học thành công")
                .build();
    }

    @GetMapping
    public ApiResponse<List<ClassResponse>> getClasses(@RequestParam(required = false) Long majorId, @RequestParam(required = false) Integer academicYear) {
        return ApiResponse.<List<ClassResponse>>builder()
                .result(classService.getClasses(majorId, academicYear))
                .build();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('admin') or hasRole('ADMIN')")
    public ApiResponse<ClassResponse> createClass(@RequestBody @Valid ClassRequest request) {
        return ApiResponse.<ClassResponse>builder()
                .result(classService.createClass(request))
                .message("Tạo lớp học thành công")
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('admin') or hasRole('ADMIN')")
    public ApiResponse<ClassResponse> updateClass(@PathVariable Long id, @RequestBody @Valid ClassRequest request) {
        return ApiResponse.<ClassResponse>builder()
                .result(classService.updateClass(id, request))
                .message("Cập nhật lớp học thành công")
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('admin') or hasRole('ADMIN')")
    public ApiResponse<String> deleteClass(@PathVariable Long id) {
        classService.deleteClass(id);
        return ApiResponse.<String>builder()
                .message("Xóa lớp học thành công")
                .build();
    }
}