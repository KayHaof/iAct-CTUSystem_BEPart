package com.example.common.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "departments")
@Data
public class Departments {
    @Id
    private Long id;
    private String name;
    private String description;
}

