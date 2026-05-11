package com.example.activityservice.feature.categories.dto;

import lombok.Data;

@Data
public class CategoryRequest {
    private Long parentId;
    private String code;
    private String name;
    private Integer maxPoint;
}