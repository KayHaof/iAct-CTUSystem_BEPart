package com.example.feature.user_profile.dto;

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
    private Integer roleType;     // 1: SV, 2: Khoa
    private String studentCode;   // Chỉ dùng cho SV
    private Long classId;
    private String classCode;// Chỉ dùng cho SV
    private String description;
}
