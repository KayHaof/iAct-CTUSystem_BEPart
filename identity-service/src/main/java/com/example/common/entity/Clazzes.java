package com.example.common.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "classes") // Map vào bảng local của Identity
@Data
@NoArgsConstructor
public class Clazzes {
    @Id
    private Long id;

    private String classCode;
    private String name;

    public Clazzes(Long id) {
        this.id = id;
    }
}
