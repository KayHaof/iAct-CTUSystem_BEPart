package com.example.activityservice.feature.users.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserSnapshot {
    private Long id;
    private Long userId;
    private String username;
    private String email;
    private String fullName;
    private String studentCode;
    private String avatarUrl;
    private Long departmentId;
    private Integer roleType;
    private Integer status;

    public Long resolvedId() {
        return id != null ? id : userId;
    }
}
