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
    private Integer roleType; // 1=student, 2=department, 3=admin, 4=other

    private Integer status; // 1=active, 0=inactive, 2=locked

    @Column(name = "student_code", unique = true)
    private String studentCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id")
    private Clazzes clazz; // Sử dụng Clazz để tránh trùng từ khóa Java

    @Column(name = "full_name")
    private String fullName;

    private LocalDate birthday;
    private Integer gender; // 0=female, 1=male, 2=other
    private String phone;
    private String address;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

//    @OneToMany(mappedBy = "student")
//    private List<Registration> registrations;
//
//    @OneToMany(mappedBy = "user")
//    private List<Notification> notifications;
}

