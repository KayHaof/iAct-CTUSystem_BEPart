package com.example.common.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "idt_local_profiles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class IdtLocalProfile {
    @Id
    @Column(name = "user_id")
    private Long userId;

    private String fullName;
    private String studentCode;
    private String avatarUrl;

    @Column(name = "department_id")
    private Long departmentId;

    @Column(name = "class_id")
    private Long classId;

    @Column(name = "class_code")
    private String classCode;

    @Column(name = "department_name")
    private String departmentName;
}
