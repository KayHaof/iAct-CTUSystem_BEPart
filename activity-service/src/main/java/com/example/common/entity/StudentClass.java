package com.example.common.entity;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "classes")
@Getter
public class StudentClass {
    @Id
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "major_id", insertable = false, updatable = false)
    private Majors major;
}