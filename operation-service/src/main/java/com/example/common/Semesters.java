package com.example.common;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "semesters")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Semesters {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String semesterName;
    private String academicYear;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean isActive;
    private Boolean isLocked;
}
