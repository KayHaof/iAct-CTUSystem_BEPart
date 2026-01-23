package com.example.feature.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {
    private String username;
    private String password;
    private String email;
    private Integer roleType;

    private String firstName;
    private String lastName;

    private String studentCode;
    private Integer classId;

    private String phone;
    private Integer gender;
}