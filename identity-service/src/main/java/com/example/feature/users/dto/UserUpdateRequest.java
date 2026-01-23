package com.example.feature.users.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserUpdateRequest {
    private String fullName;
    private String phone;
    private String address;
    private LocalDate birthday;
    private Integer gender;

    private String studentCode;
    private Integer classId;
    private Integer departmentId;
}