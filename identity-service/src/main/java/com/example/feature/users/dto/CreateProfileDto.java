package com.example.feature.users.dto;

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
    private Long classId;
    private String description;
}