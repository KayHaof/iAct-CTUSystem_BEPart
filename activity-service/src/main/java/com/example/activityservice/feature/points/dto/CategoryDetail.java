package com.example.activityservice.feature.points.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDetail {
    private Long id;
    private String code;
    private String name;
    private Integer maxPoint;
    private Integer earnedPoint;
    private Double percentage;
    private List<CriterionDetail> criteria;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class CriterionDetail {
    private Long id;
    private String code;
    private String name;
    private Integer maxPoint;
    private Integer earnedPoint;
    private String activityName;
    private String attendedAt;
}
