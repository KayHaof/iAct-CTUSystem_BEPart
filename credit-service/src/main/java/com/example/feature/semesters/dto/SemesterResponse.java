package com.example.feature.semesters.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class SemesterResponse {
    private Long id;
    private String semesterName;
    private String academicYear;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean isActive;
    private Boolean isLocked;
    private LocalDateTime createdAt;
}
