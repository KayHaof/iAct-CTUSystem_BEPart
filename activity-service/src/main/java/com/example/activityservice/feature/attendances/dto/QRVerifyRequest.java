package com.example.activityservice.feature.attendances.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QRVerifyRequest {
    private String qrData;
    private Long activityId;
    private Long sessionId;
    private Long registrationId;
}
