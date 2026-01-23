package com.example.feature.major.model;

import com.example.feature.classes.model.Clazzes;
import com.example.feature.departments.model.Departments;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "major")
@Data
public class Majors {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(name = "program_type")
    private String programType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Departments department;

    @OneToMany(mappedBy = "major")
    private List<Clazzes> classes;
}