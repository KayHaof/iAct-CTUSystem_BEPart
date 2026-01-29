package com.example.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity(name = "commonUsers")
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
    private Integer roleType; // 1=student, 2=department, 3=admin, 4=other

    private Integer status; // 1=active, 0=inactive, 2=locked

    @Column(name = "student_code", unique = true)
    private String studentCode;


    @Column(name = "full_name")
    private String fullName;

    private LocalDate birthday;
    private Integer gender; // 0=female, 1=male, 2=other
    private String phone;
    private String address;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

}
