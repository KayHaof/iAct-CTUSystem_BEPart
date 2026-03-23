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
public class UserUpdateRequest {
    private String fullName;
    private String phone;
    private String address;
    private String avatarUrl;
    private LocalDate birthday;
    private Integer gender;
    private Long classId;
    private Long departmentId;
}