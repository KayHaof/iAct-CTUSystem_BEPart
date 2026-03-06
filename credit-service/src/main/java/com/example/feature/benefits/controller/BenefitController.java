package com.example.feature.benefits.controller;

import com.example.dto.ApiResponse;
import com.example.feature.benefits.dto.BenefitRequest;
import com.example.feature.benefits.dto.BenefitResponse;
import com.example.feature.benefits.service.BenefitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/benefits")
@RequiredArgsConstructor
public class BenefitController {

    private final BenefitService benefitService;

    // --- TẠO MỚI ---
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPARTMENT')")
    public ResponseEntity<ApiResponse<BenefitResponse>> createBenefit(@RequestBody BenefitRequest request) {
        return new ResponseEntity<>(
                ApiResponse.success(benefitService.createBenefit(request)),
                HttpStatus.CREATED
        );
    }

    // --- LẤY THEO ACTIVITY_ID ---
    @GetMapping("/activity/{activityId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<BenefitResponse>>> getBenefitsByActivity(@PathVariable Long activityId) {
        return ResponseEntity.ok(ApiResponse.success(benefitService.getBenefitsByActivityId(activityId)));
    }

    // --- CHI TIẾT ---
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BenefitResponse>> getBenefitById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(benefitService.getBenefitById(id)));
    }

    // --- CẬP NHẬT ---
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPARTMENT')")
    public ResponseEntity<ApiResponse<BenefitResponse>> updateBenefit(
            @PathVariable Long id,
            @RequestBody BenefitRequest request) {
        return ResponseEntity.ok(ApiResponse.success(benefitService.updateBenefit(id, request)));
    }

    // --- XÓA ---
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPARTMENT')")
    public ResponseEntity<ApiResponse<Void>> deleteBenefit(@PathVariable Long id) {
        benefitService.deleteBenefit(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
