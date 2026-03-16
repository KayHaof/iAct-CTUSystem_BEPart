package com.example.feature.attendances.controller;

import com.example.dto.ApiResponse;
import com.example.feature.attendances.dto.AttendanceResponse;
import com.example.feature.attendances.dto.CheckInRequest;
import com.example.feature.attendances.service.AttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/attendances")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/check-in")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<AttendanceResponse> checkIn(@RequestBody @Valid CheckInRequest request) {
        return ApiResponse.<AttendanceResponse>builder()
                .result(attendanceService.checkIn(request))
                .build();
    }
}