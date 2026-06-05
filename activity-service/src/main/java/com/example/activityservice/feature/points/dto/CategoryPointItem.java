package com.example.activityservice.feature.points.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryPointItem {
    private Long categoryId;
    private String categoryCode;
    private String categoryName;
    private Integer earnedPoint;
    private Integer maxPoint;
    private Double percentage;
}
