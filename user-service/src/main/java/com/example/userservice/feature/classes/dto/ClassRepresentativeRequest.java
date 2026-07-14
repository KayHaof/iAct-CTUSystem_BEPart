package com.example.userservice.feature.classes.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ClassRepresentativeRequest {
    private Long classId;
    private Long studentId;
    private String representativeType;
    private LocalDate startDate;
    private LocalDate endDate;
}
