package com.example.userservice.feature.user_profile.model;

import com.example.userservice.feature.departments.model.Departments;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "department_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentProfile {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Departments department;

    @Column(name = "full_name")
    private String fullName;

    private String phone;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "address")
    private String address;
}
