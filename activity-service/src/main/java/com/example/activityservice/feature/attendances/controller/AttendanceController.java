package com.example.activityservice.feature.attendances.controller;

import com.example.activityservice.feature.attendances.dto.AttendanceResponse;
import com.example.activityservice.feature.attendances.dto.AttendanceStatisticsResponse;
import com.example.activityservice.feature.attendances.dto.CheckInRequest;
import com.example.activityservice.feature.attendances.dto.FaceCheckInRequest;
import com.example.activityservice.feature.attendances.dto.FaceCheckInResponse;
import com.example.activityservice.feature.attendances.service.AttendanceService;
import com.example.dto.ApiResponse;
import com.example.dto.PageDTO;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/attendances")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/check-in")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<AttendanceResponse> checkIn(@RequestBody CheckInRequest request) {
        return ApiResponse.success(attendanceService.checkIn(request));
    }

    @PostMapping("/check-out")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<AttendanceResponse> checkOut(@RequestBody CheckInRequest request) {
        return ApiResponse.success(attendanceService.checkOut(request));
    }

    @PostMapping(value = "/face-check-in", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<FaceCheckInResponse> faceCheckIn(
            @RequestParam Long activityId,
            @RequestPart("liveImage") MultipartFile liveImage,
            @RequestParam(required = false) BigDecimal latitude,
            @RequestParam(required = false) BigDecimal longitude) {
        FaceCheckInRequest request = new FaceCheckInRequest();
        request.setActivityId(activityId);
        request.setLatitude(latitude);
        request.setLongitude(longitude);
        return ApiResponse.success(attendanceService.faceCheckIn(request, liveImage));
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
