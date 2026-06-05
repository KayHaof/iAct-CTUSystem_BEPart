package com.example.activityservice.feature.semesters.controller;

import com.example.activityservice.feature.semesters.dto.SemesterRequest;
import com.example.activityservice.feature.semesters.dto.SemesterResponse;
import com.example.activityservice.feature.semesters.service.SemesterService;
import com.example.dto.ApiResponse;
import jakarta.validation.Valid;
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

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<SemesterResponse>>> getAllSemesters(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Boolean locked,
            @RequestParam(required = false) String academicYear) {
        return ResponseEntity.ok(ApiResponse.success(semesterService.getAllSemesters(active, locked, academicYear)));
    }

    @GetMapping("/active")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SemesterResponse>> getActiveSemester() {
        return ResponseEntity.ok(ApiResponse.success(semesterService.getActiveSemester()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SemesterResponse>> getSemesterById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(semesterService.getSemesterById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SemesterResponse>> createSemester(@RequestBody @Valid SemesterRequest request) {
        return new ResponseEntity<>(
                ApiResponse.success(semesterService.createSemester(request)),
                HttpStatus.CREATED
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SemesterResponse>> updateSemester(
            @PathVariable Long id,
            @RequestBody @Valid SemesterRequest request) {
        return ResponseEntity.ok(ApiResponse.success(semesterService.updateSemester(id, request)));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SemesterResponse>> activateSemester(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(semesterService.activateSemester(id)));
    }

    @PatchMapping("/{id}/lock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SemesterResponse>> lockSemester(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(semesterService.lockSemester(id)));
    }

    @PatchMapping("/{id}/unlock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SemesterResponse>> unlockSemester(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(semesterService.unlockSemester(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSemester(@PathVariable Long id) {
        semesterService.deleteSemester(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
