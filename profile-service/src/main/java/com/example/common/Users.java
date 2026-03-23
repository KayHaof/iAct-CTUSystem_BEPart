package com.example.common;

import com.example.feature.classes.model.Clazzes;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Users {
    @Id
    private Long id;

    @Column(name = "keycloak_id", nullable = false, unique = true)
    private String keycloakId;

    @Column(unique = true)
    private String username;
    @Column(unique = true)
    private String email;

    @Column(name = "role_type", nullable = false)
    private Integer roleType;

    private Integer status;
}

