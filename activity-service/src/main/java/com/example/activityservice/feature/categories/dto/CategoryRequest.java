package com.example.activityservice.feature.categories.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryRequest {
    private Long parentId;

    @Size(max = 50, message = "Category code must not exceed 50 characters")
    private String code;

    @NotBlank(message = "Category name is required")
    @Size(max = 1024, message = "Category name must not exceed 1024 characters")
    private String name;

    @Min(value = 0, message = "Max point must be greater than or equal to 0")
    private Integer maxPoint;

    private Boolean isActive;
}
