package com.example.userservice.feature.keycloak.service;

public interface KeycloakUserProvisionService {

    String createUser(KeycloakUserProvisionRequest request);

    void updateUser(String keycloakId, KeycloakUserProvisionRequest request);

    void updateUserEnabled(String keycloakId, boolean enabled);

    void deleteUser(String keycloakId);
}
