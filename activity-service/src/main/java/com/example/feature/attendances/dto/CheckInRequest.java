package com.example.feature.attendances.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CheckInRequest {
    @NotNull(message = "Thiếu mã hoạt động")
    private Long activityId;

    private BigDecimal latitude;
    private BigDecimal longitude;

    @NotNull(message = "Thiếu phương thức điểm danh")
    private Integer method; // 1=QR, 2=Manual, 3=Face_ID

    private String verifyCode;
}