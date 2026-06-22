package com.example.activityservice.feature.benefits.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BenefitRequest {
    private Long activityId;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    @NotNull(message = "Benefit type is required")
    @Min(value = 1, message = "Benefit type must be between 1 and 3")
    @Max(value = 3, message = "Benefit type must be between 1 and 3")
    private Integer type;

    @NotNull(message = "Point is required")
    @Min(value = 0, message = "Point must be greater than or equal to 0")
    private Integer point;
}
