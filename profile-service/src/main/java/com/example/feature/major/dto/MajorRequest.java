package com.example.feature.major.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MajorRequest {
    @NotBlank(message = "Tên chuyên ngành không được để trống")
    private String name;

    private String programType;

    @NotNull(message = "ID Khoa/Đơn vị không được để trống")
    private Long departmentId;
}
