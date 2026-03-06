package com.example.feature.categories.dto;

import lombok.Data;

@Data
public class CategoryRequest {
    private Long parentId; // Truyền null nếu là danh mục gốc (Level 1)
    private String code;
    private String name;
    private Integer maxPoint;
}