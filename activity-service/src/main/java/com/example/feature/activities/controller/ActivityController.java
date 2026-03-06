package com.example.feature.activities.controller;

import com.example.dto.ApiResponse;
import com.example.feature.activities.dto.ActivityApprovalRequest;
import com.example.feature.activities.dto.ActivityRequest;
import com.example.feature.activities.dto.ActivityResponse;
import com.example.feature.activities.service.ActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<ApiResponse<List<ActivityResponse>>> getAllActivities(
            @RequestParam(value = "status", required = false) Integer status
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(activityService.getAllActivities(status))
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
        // Trả về 200 OK kèm theo ApiResponse (result = null) để Frontend parse JSON không bị lỗi
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // --- APPROVE ---
    @PutMapping("/{id}/approval")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<ActivityResponse>> approveActivity(
            @PathVariable Long id,
            @RequestBody ActivityApprovalRequest request) {
        return ResponseEntity.ok(ApiResponse.success(activityService.approveActivity(id, request)));
    }

}