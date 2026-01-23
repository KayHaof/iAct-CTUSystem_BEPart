package com.example.feature.users.dto;

import lombok.*;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {
    private String username;
    private String email;
    private String fullName;
    private String studentCode;
    private Integer roleType;

    private String classCode;
    private String departmentName;
}