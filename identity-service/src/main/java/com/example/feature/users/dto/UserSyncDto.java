package com.example.feature.users.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserSyncDto {
    private String keycloakId;
    private String username;
    private String email;
    private String fullName;
    private Integer status;
    private Integer roleType;
}