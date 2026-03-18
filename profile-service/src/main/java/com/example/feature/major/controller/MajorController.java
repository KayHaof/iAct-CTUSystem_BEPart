package com.example.feature.major.controller;

import com.example.dto.ApiResponse;
import com.example.feature.major.dto.MajorRequest;
import com.example.feature.major.dto.MajorResponse;
import com.example.feature.major.service.MajorService;
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
    public ApiResponse<List<MajorResponse>> getMajors(@RequestParam(required = false) Long departmentId) {
        return ApiResponse.<List<MajorResponse>>builder()
                .result(majorService.getMajors(departmentId))
                .build();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('admin') or hasRole('ADMIN')")
    public ApiResponse<MajorResponse> createMajor(@RequestBody @Valid MajorRequest request) {
        return ApiResponse.<MajorResponse>builder()
                .result(majorService.createMajor(request))
                .message("Tạo chuyên ngành thành công")
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('admin') or hasRole('ADMIN')")
    public ApiResponse<MajorResponse> updateMajor(@PathVariable Long id, @RequestBody @Valid MajorRequest request) {
        return ApiResponse.<MajorResponse>builder()
                .result(majorService.updateMajor(id, request))
                .message("Cập nhật chuyên ngành thành công")
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('admin') or hasRole('ADMIN')")
    public ApiResponse<String> deleteMajor(@PathVariable Long id) {
        majorService.deleteMajor(id);
        return ApiResponse.<String>builder()
                .message("Xóa chuyên ngành thành công")
                .build();
    }
}