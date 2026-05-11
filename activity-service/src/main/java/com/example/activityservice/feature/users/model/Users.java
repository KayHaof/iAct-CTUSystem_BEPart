package com.example.activityservice.feature.users.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Users {
    @Id
    private Long id;

    @Column(name = "username")
    private String username;

    @Column(name = "email")
    private String email;

    @Column(name = "student_code")
    private String studentCode;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "department_id")
    private Long departmentId;
}
