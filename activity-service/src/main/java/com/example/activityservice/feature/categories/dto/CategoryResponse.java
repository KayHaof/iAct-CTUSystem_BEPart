package com.example.activityservice.feature.categories.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryResponse {
    private Long id;
    private String code;
    private String name;
    private Integer maxPoint;
    private Long parentId;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private List<CategoryResponse> children;
}
