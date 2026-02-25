package com.example.feature.users.model;

import com.example.common.Clazzes;
import com.example.common.Departments;
import com.example.common.Notifications;
import com.example.common.Registrations;
import jakarta.persistence.Entity;
import jakarta.persistence.*;
import jakarta.persistence.Table;
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
@Builder
public class Users {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id")
    private Clazzes clazz;

    @Column(name = "full_name")
    private String fullName;

    private LocalDate birthday;
    private Integer gender; // 0=female, 1=male, 2=other
    private String phone;
    private String address;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Departments department;

    @OneToMany(mappedBy = "student")
    private List<Registrations> registrations;

    @OneToMany(mappedBy = "user")
    private List<Notifications> notifications;
}
