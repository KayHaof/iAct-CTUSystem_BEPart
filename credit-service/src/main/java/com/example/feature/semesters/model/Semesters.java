package com.example.feature.semesters.model;

import com.example.common.Activities;
import com.example.common.Complaints;
import com.example.feature.student_awards.model.Student_awards;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "semesters")
@Data
public class Semesters {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "semester_name")
    private String semesterName;

    @Column(name = "academic_year")
    private String academicYear;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "is_active")
    private Boolean isActive = false;

    @Column(name = "is_locked")
    private Boolean isLocked = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Các liên kết ngược
    @OneToMany(mappedBy = "semester")
    private List<Activities> activities;

    @OneToMany(mappedBy = "semester")
    private List<Complaints> complaints;

    @OneToMany(mappedBy = "semester")
    private List<Student_awards> studentAwards;
}
