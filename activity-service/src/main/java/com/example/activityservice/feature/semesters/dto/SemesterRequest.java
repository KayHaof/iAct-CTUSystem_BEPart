package com.example.activityservice.feature.semesters.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDate;

@Data
public class SemesterRequest {
    @Size(max = 100, message = "Semester name must not exceed 100 characters")
    private String name;

    @Size(max = 100, message = "Semester name must not exceed 100 characters")
    private String semesterName;

    @Size(max = 20, message = "Academic year must not exceed 20 characters")
    @Pattern(
            regexp = "^(K\\d{2}|\\d{4}|\\d{4}-\\d{4})$",
            message = "Academic year must use format K48, 2026, or 2026-2027"
    )
    private String academicYear;

    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean isActive;
    private Boolean isLocked;
}
