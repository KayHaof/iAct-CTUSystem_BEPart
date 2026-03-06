package com.example.feature.registration.controller;

import com.example.dto.ApiResponse;
import com.example.feature.registration.dto.RegistrationRequest;
import com.example.feature.registration.dto.RegistrationResponse;
import com.example.feature.registration.service.RegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
            @RequestBody java.util.Map<String, String> payload) { // Hứng từ Body

        String reason = payload.get("reason"); // Lấy lý do ra

        return ApiResponse.<RegistrationResponse>builder()
                .result(registrationService.cancelByActivityId(activityId, reason))
                .build();
    }

    @GetMapping("/activity/{activityId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPARTMENT')")
    public ApiResponse<List<RegistrationResponse>> getParticipants(@PathVariable Long activityId) {
        return ApiResponse.<List<RegistrationResponse>>builder()
                .result(registrationService.getByActivity(activityId))
                .build();
    }
}