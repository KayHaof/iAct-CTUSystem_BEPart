package com.example.feature.user_profile.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileDto {
    // Thông tin chung
    private Long userId;
    private String fullName;
    private String phone;
    private String avatarUrl;

    // Thông tin Sinh viên
    private String studentCode;
    private LocalDate birthday;
    private Integer gender;
    private String address;
    private Long classId;
    private String classCode;
    private String className;

    // Thông tin Khoa/Đơn vị
    private Long departmentId;
    private String departmentName;
}
