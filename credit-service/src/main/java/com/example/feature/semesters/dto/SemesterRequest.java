package com.example.feature.semesters.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class SemesterRequest {
    private String semesterName;
    private String academicYear;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean isActive;
    private Boolean isLocked;
}
