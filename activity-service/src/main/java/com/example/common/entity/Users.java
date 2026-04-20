package com.example.common.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Users {

    @Id
    private Long id;

    @Column(unique = true, length = 100)
    private String username;

    @Column(unique = true)
    private String email;

}