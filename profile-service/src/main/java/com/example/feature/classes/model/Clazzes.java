package com.example.feature.classes.model;

import com.example.common.Users;
import com.example.feature.major.model.Majors;
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
    private Majors major;

    @Column(name = "academic_year")
    private Integer academicYear;

    @OneToMany(mappedBy = "clazz")
    private List<Users> students;
}


