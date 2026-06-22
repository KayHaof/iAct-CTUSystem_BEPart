package com.example.userservice.feature.keycloak.service;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class KeycloakUserProvisionRequest {
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String password;
    private String roleName;
    private Boolean enabled;
}
