package com.example.common.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "semesters")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Semesters {

    @Id
    private Long id;

    private String semesterName;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean isActive;
}