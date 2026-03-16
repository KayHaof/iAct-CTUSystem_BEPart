package com.example.common.entity;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "major")
@Getter
public class Majors {
    @Id
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", insertable = false, updatable = false)
    private Departments department;
}