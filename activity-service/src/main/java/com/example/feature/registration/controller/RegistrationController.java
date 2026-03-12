package com.example.feature.registration.controller;

import com.example.dto.ApiResponse;
import com.example.dto.PageDTO; // Nhớ import PageDTO của ní
import com.example.feature.registration.dto.RegistrationRequest;
import com.example.feature.registration.dto.RegistrationResponse;
import com.example.feature.registration.service.RegistrationService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/registrations")
@RequiredArgsConstructor
public class RegistrationController {
    private final RegistrationService registrationService;

    @GetMapping("/my-status/{activityId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<RegistrationResponse> getMyRegistrationStatus(@PathVariable Long activityId) {
        return ApiResponse.<RegistrationResponse>builder()
                .result(registrationService.getMyStatusByActivity(activityId))
                .build();
    }

    @PostMapping("/join")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<RegistrationResponse> register(@RequestBody @Valid RegistrationRequest request) {
        return ApiResponse.<RegistrationResponse>builder()
                .result(registrationService.register(request))
                .build();
    }

    @PatchMapping("/cancel-by-activity/{activityId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<RegistrationResponse> cancelByActivity(
            @PathVariable Long activityId,
            @RequestBody Map<String, String> payload) {
        String reason = payload.get("reason");
        return ApiResponse.<RegistrationResponse>builder()
                .result(registrationService.cancelByActivityId(activityId, reason))
                .build();
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or (hasRole('DEPARTMENT') and @activitySecurity.hasActivityPermission(authentication, #activityId))")
    public ApiResponse<PageDTO<RegistrationResponse>> getParticipants(
            @RequestParam Long activityId,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "ALL") String status,
            @PageableDefault(sort = "registeredAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return ApiResponse.<PageDTO<RegistrationResponse>>builder()
                .result(registrationService.getParticipants(activityId, keyword, status, pageable))
                .build();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('DEPARTMENT') and @activitySecurity.hasRegistrationPermission(authentication, #id))")
    public ApiResponse<RegistrationResponse> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> payload) {

        Integer status = payload.get("status");
        return ApiResponse.<RegistrationResponse>builder()
                .result(registrationService.updateStatus(id, status))
                .build();
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
}