package com.example.feature.activities.controller;

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
    public ResponseEntity<ActivityResponse> createActivity(@RequestBody ActivityRequest request) {
        ActivityResponse response = activityService.createActivity(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // --- READ ALL ---
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ActivityResponse>> getAllActivities() {
        return ResponseEntity.ok(activityService.getAllActivities());
    }

    // --- READ ONE ---
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ActivityResponse> getActivityById(@PathVariable Long id) {
        return ResponseEntity.ok(activityService.getActivityById(id));
    }

    // --- UPDATE ---
    @PutMapping("/{id}")
    @PreAuthorize("@activitySecurity.hasActivityPermission(authentication, #id)")
    public ResponseEntity<ActivityResponse> updateActivity(
            @PathVariable Long id,
            @RequestBody ActivityRequest request) {
        return ResponseEntity.ok(activityService.updateActivity(id, request));
    }

    // --- DELETE ---
    @DeleteMapping("/{id}")
    @PreAuthorize("@activitySecurity.hasActivityPermission(authentication, #id)")
    public ResponseEntity<String> deleteActivity(@PathVariable Long id) {
        activityService.deleteActivity(id);
        return ResponseEntity.ok("The activity was deleted successfully !");
    }

    @PutMapping("/{id}/approval")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ActivityResponse> approveActivity(
            @PathVariable Long id,
            @RequestBody ActivityApprovalRequest request) {
        return ResponseEntity.ok(activityService.approveActivity(id, request));
    }


}