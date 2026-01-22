package com.example.feature.users.model;

import com.example.common.Activities;
import com.example.common.Organizers;
import com.example.common.Students;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "keycloak_id", nullable = false, unique = true)
    private String keycloakId;

    @Column(unique = true, length = 100)
    private String username;

    @Column(unique = true)
    private String email;

    @Column(name = "role_type", nullable = false)
    private Integer roleType;

    private Integer status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "user")
    private Students students;

    @OneToOne(mappedBy = "user")
    private Organizers organizers;

    @OneToMany(mappedBy = "createdBy")
    private List<Activities> activities;
}
