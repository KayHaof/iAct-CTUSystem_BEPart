package com.example.feature.activities.controller;

import com.example.dto.ApiResponse;
import com.example.dto.PageDTO;
import com.example.feature.activities.dto.*;
import com.example.feature.activities.service.ActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    // --- CREATE ---
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPARTMENT', 'OTHER')")
    public ResponseEntity<ApiResponse<ActivityResponse>> createActivity(@RequestBody ActivityRequest request) {
        ActivityResponse response = activityService.createActivity(request);
        return new ResponseEntity<>(ApiResponse.success(response), HttpStatus.CREATED);
    }

    // --- READ ALL ---
    @GetMapping
    public ResponseEntity<ApiResponse<PageDTO<ActivityResponse>>> getAllActivities(
            @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
            @RequestParam(value = "level", required = false, defaultValue = "ALL") String level,
            @RequestParam(value = "status", required = false, defaultValue = "ALL") String status,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "size", required = false, defaultValue = "5") int size,
            @RequestParam(value = "departmentId", required = false, defaultValue = "") Long departmentId
    ) {
        int pageNumber = page > 0 ? page - 1 : 0;
        Pageable customPageable = PageRequest.of(pageNumber, size, Sort.by(Sort.Direction.DESC, "id"));
        return ResponseEntity.ok(
                ApiResponse.success(activityService.getAllActivities(keyword, level, status, departmentId, customPageable))
        );
    }

    // --- READ ONE ---
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ActivityResponse>> getActivityById(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.success(activityService.getActivityById(id))
        );
    }

    // --- GET TIMES AND LOCATION BY ID ---
    @GetMapping("/{id}/times-location")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ActivityTimeLocationResponse>> getActivityTimesAndLocation(@PathVariable Long id) {
        ActivityTimeLocationResponse timeResponse = activityService.getActivityTimesAndLocation(id);
        return ResponseEntity.ok(ApiResponse.<ActivityTimeLocationResponse>builder()
                .code(200)
                .message("Lấy thời gian hoạt động thành công")
                .result(timeResponse)
                .build());
    }

    // --- UPDATE ---
    @PutMapping("/{id}")
    @PreAuthorize("@activitySecurity.hasActivityPermission(authentication, #id)")
    public ResponseEntity<ApiResponse<ActivityResponse>> updateActivity(
            @PathVariable Long id,
            @RequestBody ActivityRequest request) {
        return ResponseEntity.ok(ApiResponse.success(activityService.updateActivity(id, request)));
    }

    // --- DELETE ---
    @DeleteMapping("/{id}")
    @PreAuthorize("@activitySecurity.hasActivityPermission(authentication, #id)")
    public ResponseEntity<ApiResponse<Void>> deleteActivity(@PathVariable Long id) {
        activityService.deleteActivity(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // --- APPROVE ---
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> approveActivity(@PathVariable Long id) {
        activityService.approveActivity(id);
        return ResponseEntity.ok(ApiResponse.success("Phê duyệt hoạt động thành công!"));
    }

    // --- REJECT ---
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> rejectActivity(
            @PathVariable Long id,
            @RequestBody ActivityReasonRequest request) {
        activityService.rejectActivity(id, request.getReason());
        return ResponseEntity.ok(ApiResponse.success("Từ chối hoạt động thành công!"));
    }

    // --- CANCEL ---
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> cancelActivity(
            @PathVariable Long id,
            @RequestBody ActivityReasonRequest request) {
        activityService.cancelActivity(id, request.getReason());
        return ResponseEntity.ok(ApiResponse.success("Hủy hoạt động thành công!"));
    }

    // --- GENERATE QR CODE ---
    @GetMapping("/{id}/qr-code")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPARTMENT')")
    public ResponseEntity<ApiResponse<String>> getActivityQrCode(@PathVariable Long id) {
        String qrImageBase64 = activityService.getQrCodeForActivity(id);
        return ResponseEntity.ok(ApiResponse.success(qrImageBase64));
    }

    // --- GET COUNT ACT BY STATUS ---
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<ActivityStatsResponse>> getActivityStats() {
        ActivityStatsResponse stats = activityService.getActivityStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}