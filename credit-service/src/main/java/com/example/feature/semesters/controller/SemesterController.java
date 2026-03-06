package com.example.feature.semesters.controller;

import com.example.dto.ApiResponse;
import com.example.feature.semesters.dto.SemesterRequest;
import com.example.feature.semesters.dto.SemesterResponse;
import com.example.feature.semesters.service.SemesterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/semesters")
@RequiredArgsConstructor
public class SemesterController {

    private final SemesterService semesterService;

    // Lấy tất cả (Ai cũng xem được)
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<SemesterResponse>>> getAllSemesters() {
        return ResponseEntity.ok(ApiResponse.success(semesterService.getAllSemesters()));
    }

    // Lấy học kỳ đang Active
    @GetMapping("/active")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SemesterResponse>> getActiveSemester() {
        return ResponseEntity.ok(ApiResponse.success(semesterService.getActiveSemester()));
    }

    // Lấy chi tiết
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SemesterResponse>> getSemesterById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(semesterService.getSemesterById(id)));
    }

    // Tạo mới (Chỉ Admin)
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SemesterResponse>> createSemester(@RequestBody SemesterRequest request) {
        return new ResponseEntity<>(
                ApiResponse.success(semesterService.createSemester(request)),
                HttpStatus.CREATED
        );
    }

    // Cập nhật (Chỉ Admin)
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SemesterResponse>> updateSemester(
            @PathVariable Long id,
            @RequestBody SemesterRequest request) {
        return ResponseEntity.ok(ApiResponse.success(semesterService.updateSemester(id, request)));
    }

    // Xóa (Chỉ Admin)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSemester(@PathVariable Long id) {
        semesterService.deleteSemester(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
