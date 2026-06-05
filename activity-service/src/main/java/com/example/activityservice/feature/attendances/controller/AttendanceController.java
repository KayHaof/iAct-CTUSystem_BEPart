package com.example.activityservice.feature.attendances.controller;

import com.example.activityservice.feature.attendances.dto.AttendanceResponse;
import com.example.activityservice.feature.attendances.dto.AttendanceStatisticsResponse;
import com.example.activityservice.feature.attendances.dto.CheckInRequest;
import com.example.activityservice.feature.attendances.dto.QRVerifyRequest;
import com.example.activityservice.feature.attendances.service.AttendanceService;
import com.example.dto.ApiResponse;
import com.example.dto.PageDTO;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
        return ApiResponse.success(attendanceService.checkIn(request));
    }

    @GetMapping("/activity/{activityId}/session/{sessionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPARTMENT')")
    public ApiResponse<PageDTO<AttendanceResponse>> getAttendancesBySession(
            @PathVariable Long activityId,
            @PathVariable Long sessionId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "checkinTime"));
        return ApiResponse.success(attendanceService.getAttendancesBySession(activityId, sessionId, pageable));
    }

    @PostMapping("/verify-qr")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPARTMENT')")
    public ApiResponse<AttendanceResponse> verifyQRCode(@RequestBody QRVerifyRequest request) {
        return ApiResponse.success(attendanceService.verifyAndCheckIn(request));
    }

    @GetMapping("/activity/{activityId}/session/{sessionId}/export")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPARTMENT')")
    public void exportAttendanceExcel(
            @PathVariable Long activityId,
            @PathVariable Long sessionId,
            HttpServletResponse response) throws Exception {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
                "attachment; filename=Danh_sach_Diem_Danh_" + activityId + "_Buoi_" + sessionId + ".xlsx");
        attendanceService.exportAttendanceToExcel(activityId, sessionId, response.getOutputStream());
    }

    @GetMapping("/activity/{activityId}/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPARTMENT')")
    public ApiResponse<AttendanceStatisticsResponse> getAttendanceStatistics(
            @PathVariable Long activityId,
            @PathVariable(required = false) Long sessionId) {
        return ApiResponse.success(attendanceService.getStatistics(activityId, sessionId));
    }
}
