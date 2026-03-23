package com.example.feature.user_profile.model;
import com.example.feature.classes.model.Clazzes;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "student_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentProfile {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "student_code", unique = true, nullable = false)
    private String studentCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id")
    private Clazzes clazz;

    @Column(name = "full_name")
    private String fullName;

    private LocalDate birthday;
    private Integer gender;
    private String phone;
    private String address;

    @Column(name = "avatar_url")
    private String avatarUrl;
}