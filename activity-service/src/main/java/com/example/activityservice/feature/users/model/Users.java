package com.example.activityservice.feature.users.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "users")
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

    @Column(name = "class_id")
    private Long classId;

    @Column(name = "class_code")
    private String classCode;

    @Column(name = "class_name")
    private String className;

    @Column(name = "academic_year")
    private String academicYear;

    @Column(name = "department_id")
    private Long departmentId;

    @Column(name = "role_type")
    private Integer roleType;

    @Column(name = "status")
    private Integer status;

    public Users() {
    }

}
