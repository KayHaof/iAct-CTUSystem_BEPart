package com.example.feature.student_awards.model;

import com.example.common.Users;
import com.example.feature.awards.model.Awards;
import com.example.feature.semesters.model.Semesters;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "student_awards")
@Data
public class Student_awards {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Users student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "award_id")
    private Awards award;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id")
    private Semesters semester;

    private Integer status; // 0=nominated, 1=approved, 2=rejected
}

