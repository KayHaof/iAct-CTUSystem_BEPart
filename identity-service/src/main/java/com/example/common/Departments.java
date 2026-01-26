package com.example.common;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Table(name = "departments")
@Data
public class Departments {
    @Id
    private Long id;
    private String name;
    private String description;
}

