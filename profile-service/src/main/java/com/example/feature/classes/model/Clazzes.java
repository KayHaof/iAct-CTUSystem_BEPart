package com.example.feature.classes.model;

import com.example.feature.major.model.Major;
import com.example.feature.user_profile.model.StudentProfile;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "classes")
@Data
public class Clazzes {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "class_code", unique = true)
    private String classCode;

    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "major_id")
    private Major major;

    @Column(name = "academic_year")
    private Integer academicYear;

    @OneToMany(mappedBy = "clazz")
    private List<StudentProfile> students;
}