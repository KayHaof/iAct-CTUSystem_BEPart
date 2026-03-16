package com.example.common.entity;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "departments")
@Getter
public class Departments {
    @Id
    private Long id;

    private String name;
}