package com.example.userservice.feature.user_profile.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateProfileDto {
    private Long userId;
    private String fullName;
    private Integer roleType;
    private String studentCode;
    private Long departmentId;
    private Long classId;
    private String classCode;
    private String description;
    private String phone;
    private String address;
    private String avatarUrl;
}
