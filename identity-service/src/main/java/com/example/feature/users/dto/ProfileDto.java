package com.example.feature.users.dto;

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
    // THÔNG TIN CHUNG (Role nào cũng có)
    private Long userId;
    private String fullName;
    private String phone;
    private String avatarUrl;

    // THÔNG TIN RIÊNG CỦA SINH VIÊN (Role 1)
    private String studentCode;
    private LocalDate birthday;
    private Integer gender;
    private String address;
    private Long classId;
    private String classCode;
    private String className;

    // THÔNG TIN RIÊNG CỦA KHOA / ĐƠN VỊ (Role 2)
    private Long departmentId;
    private String departmentName;
}