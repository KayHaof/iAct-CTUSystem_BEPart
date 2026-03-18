package com.example.feature.classes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ClassRequest {
    @NotBlank(message = "Mã lớp không được để trống")
    private String classCode;

    @NotBlank(message = "Tên lớp không được để trống")
    private String name;

    private Integer academicYear;

    @NotNull(message = "ID Chuyên ngành không được để trống")
    private Long majorId;
}
