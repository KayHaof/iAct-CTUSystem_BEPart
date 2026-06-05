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
public class PointDetailsResponse {
    private Long studentId;
    private Long semesterId;
    private Integer totalPoint;
    private Integer maxPoint;
    private List<CategoryDetail> categories;
}
