package com.example.feature.categories.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class CategoryResponse {
    private Long id;
    private Long parentId;
    private String code;
    private String name;
    private Integer maxPoint;

    private List<CategoryResponse> children;
}