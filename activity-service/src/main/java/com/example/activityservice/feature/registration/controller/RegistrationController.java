package com.example.activityservice.feature.registration.controller;

import com.example.activityservice.feature.registration.dto.RegistrationQRResponse;
import com.example.activityservice.feature.registration.dto.RegistrationRequest;
import com.example.activityservice.feature.registration.dto.RegistrationResponse;
import com.example.activityservice.feature.registration.service.RegistrationService;
import com.example.dto.ApiResponse;
import com.example.dto.PageDTO;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/registrations")
@RequiredArgsConstructor
public class RegistrationController {
    private final RegistrationService registrationService;

    @GetMapping("/my-status/{activityId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<RegistrationResponse> getMyRegistrationStatus(@PathVariable Long activityId) {
        return ApiResponse.success(registrationService.getMyStatusByActivity(activityId));
    }

    @PostMapping("/join")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<RegistrationResponse> register(@RequestBody @Valid RegistrationRequest request) {
        return ApiResponse.success(registrationService.register(request));
    }

    @PatchMapping("/cancel-by-activity/{activityId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<RegistrationResponse> cancelByActivity(
            @PathVariable Long activityId,
            @RequestBody Map<String, String> payload) {
        String reason = payload.get("reason");
        return ApiResponse.success(registrationService.cancelByActivityId(activityId, reason));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or (hasRole('DEPARTMENT') and @activitySecurity.hasActivityPermission(authentication, #activityId))")
    public ApiResponse<PageDTO<RegistrationResponse>> getParticipants(
            @RequestParam Long activityId,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "ALL") String status,
            @PageableDefault(sort = "registeredAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(registrationService.getParticipants(activityId, keyword, status, pageable));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('DEPARTMENT') and @activitySecurity.hasRegistrationPermission(authentication, #id))")
    public ApiResponse<RegistrationResponse> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> payload) {
        Integer status = payload.get("status");
        return ApiResponse.success(registrationService.updateStatus(id, status));
    }

    @GetMapping("/export")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('DEPARTMENT') and @activitySecurity.hasActivityPermission(authentication, #activityId))")
    public void exportExcel(
            @RequestParam Long activityId,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "ALL") String status,
            HttpServletResponse response) throws Exception {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=Danh_sach_SV_HoatDong_" + activityId + ".xlsx");
        registrationService.exportToExcel(activityId, keyword, status, response.getOutputStream());
    }

    @GetMapping("/my-records")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<List<RegistrationResponse>> getMyRecords(
            @RequestParam(required = false) Long semesterId) {
        return ApiResponse.success(registrationService.getMyRecords(semesterId));
    }

    @GetMapping("/{id}/qr")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<RegistrationQRResponse> getRegistrationQR(@PathVariable Long id) {
        return ApiResponse.success(registrationService.getQRCode(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<RegistrationResponse> cancelRegistration(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> payload) {
        String reason = payload != null ? payload.get("reason") : null;
        return ApiResponse.success(registrationService.cancel(id, reason));
    }

    @PutMapping("/{id}/sessions")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<RegistrationResponse> updateRegistrationSessions(
            @PathVariable Long id,
            @RequestBody List<Long> sessionIds) {
        return ApiResponse.success(registrationService.updateSessions(id, sessionIds));
    }
}
