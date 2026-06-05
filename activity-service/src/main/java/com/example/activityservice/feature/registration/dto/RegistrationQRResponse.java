package com.example.activityservice.feature.registration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationQRResponse {
    private Long registrationId;
    private Long activityId;
    private String activityTitle;
    private String qrData;  // Encrypted QR data
    private String checkInCode;
    private Long validUntil;  // Timestamp
    private SessionInfo sessionInfo;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionInfo {
        private Long sessionId;
        private String sessionName;
        private LocalDateTime checkInTime;
        private LocalDateTime checkOutTime;
    }
}
