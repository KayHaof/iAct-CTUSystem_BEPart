package com.example.feature.benefits.dto;

import lombok.Data;

@Data
public class BenefitRequest {
    private Long activityId;
    private Long categoryId;
    private Integer type;
    private Integer point;
}
