package com.example.activityservice.feature.benefits.dto;

import lombok.Data;

@Data
public class BenefitResponse {
    private Long id;
    private Long activityId;
    private Long categoryId;
    private String categoryName;
    private Integer type;
    private Integer point;
}