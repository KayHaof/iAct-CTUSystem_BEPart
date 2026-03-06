package com.example.feature.benefits.dto;

import lombok.Data;

@Data
public class BenefitResponse {
    private Long id;
    private Long activityId;
    private Long categoryId;
    private String categoryName; // Trả thêm tên để FE dễ hiển thị
    private Integer type;
    private Integer point;
}