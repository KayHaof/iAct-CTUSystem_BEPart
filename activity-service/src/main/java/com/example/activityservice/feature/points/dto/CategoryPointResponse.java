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
public class CategoryPointResponse {
    private Long id;
    private String code;
    private String name;
    private Integer maxPoint;
    private Integer orderIndex;
    private Long parentId;
    private List<CategoryPointResponse> children;
}
