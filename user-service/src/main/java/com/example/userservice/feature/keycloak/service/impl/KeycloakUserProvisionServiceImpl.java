package com.example.userservice.feature.keycloak.service.impl;

import com.example.exception.AppException;
import com.example.exception.ErrorCode;
import com.example.userservice.feature.keycloak.service.KeycloakUserProvisionRequest;
import com.example.userservice.feature.keycloak.service.KeycloakUserProvisionService;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakUserProvisionServiceImpl implements KeycloakUserProvisionService {

    private static final String REALM = "myRealm";

    private final Keycloak keycloak;

    @Override
    public String createUser(KeycloakUserProvisionRequest request) {
        UserRepresentation userRepresentation = buildUserRepresentation(request);
        String createdKeycloakId = null;

        try {
            Response response = keycloak.realm(REALM).users().create(userRepresentation);
            try {
                if (response.getStatus() == 201) {
                    String path = response.getLocation().getPath();
                    createdKeycloakId = path.substring(path.lastIndexOf("/") + 1);
                    assignRoleToUser(createdKeycloakId, request.getRoleName());
                    return createdKeycloakId;
                }

                if (response.getStatus() == 409) {
                    throw new AppException(ErrorCode.VALUE_EXISTED, "Username hoặc Email đã tồn tại trên Keycloak!");
                }

                throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION,
                        "Lỗi Keycloak khi tạo user: " + response.getStatus());
            } finally {
                response.close();
            }
        } catch (AppException exception) {
            rollbackCreatedUser(createdKeycloakId);
            throw exception;
        } catch (Exception exception) {
            rollbackCreatedUser(createdKeycloakId);
            throw new AppException(ErrorCode.INVALID_ACTION,
                    "Tạo người dùng trên Keycloak thất bại: " + exception.getMessage());
        }
    }

    @Override
    public void updateUser(String keycloakId, KeycloakUserProvisionRequest request) {
        try {
            UserResource userResource = keycloak.realm(REALM).users().get(keycloakId);
            UserRepresentation userRepresentation = userResource.toRepresentation();
            userRepresentation.setUsername(request.getUsername());
            userRepresentation.setEmail(request.getEmail());
            userRepresentation.setFirstName(request.getFirstName());
            userRepresentation.setLastName(request.getLastName());
            userRepresentation.setEnabled(Boolean.TRUE.equals(request.getEnabled()));
            userRepresentation.setEmailVerified(true);
            userResource.update(userRepresentation);
        } catch (AppException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AppException(ErrorCode.INVALID_ACTION,
                    "Cập nhật người dùng trên Keycloak thất bại: " + exception.getMessage());
        }
    }

    @Override
    public void updateUserEnabled(String keycloakId, boolean enabled) {
        try {
            UserResource userResource = keycloak.realm(REALM).users().get(keycloakId);
            UserRepresentation userRepresentation = userResource.toRepresentation();
            userRepresentation.setEnabled(enabled);
            userResource.update(userRepresentation);
        } catch (Exception exception) {
            throw new AppException(ErrorCode.INVALID_ACTION,
                    "Đồng bộ trạng thái người dùng trên Keycloak thất bại: " + exception.getMessage());
        }
    }

    @Override
    public void deleteUser(String keycloakId) {
        if (keycloakId == null || keycloakId.isBlank()) {
            return;
        }

        try {
            keycloak.realm(REALM).users().get(keycloakId).remove();
        } catch (Exception exception) {
            throw new AppException(ErrorCode.INVALID_ACTION,
                    "Xóa người dùng trên Keycloak thất bại: " + exception.getMessage());
        }
    }

    private UserRepresentation buildUserRepresentation(KeycloakUserProvisionRequest request) {
        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setUsername(request.getUsername());
        userRepresentation.setEmail(request.getEmail());
        userRepresentation.setFirstName(request.getFirstName());
        userRepresentation.setLastName(request.getLastName());
        userRepresentation.setEnabled(Boolean.TRUE.equals(request.getEnabled()));
        userRepresentation.setEmailVerified(true);

        CredentialRepresentation credentialRepresentation = new CredentialRepresentation();
        credentialRepresentation.setType(CredentialRepresentation.PASSWORD);
        credentialRepresentation.setValue(request.getPassword());
        credentialRepresentation.setTemporary(false);
        userRepresentation.setCredentials(Collections.singletonList(credentialRepresentation));
        return userRepresentation;
    }

    private void assignRoleToUser(String userId, String roleName) {
        var roleRepresentation = keycloak.realm(REALM).roles().get(roleName).toRepresentation();
        keycloak.realm(REALM).users().get(userId).roles().realmLevel()
                .add(Collections.singletonList(roleRepresentation));
    }

    private void rollbackCreatedUser(String keycloakId) {
        if (keycloakId == null || keycloakId.isBlank()) {
            return;
        }

        try {
            keycloak.realm(REALM).users().get(keycloakId).remove();
            log.info("Đã rollback user trên Keycloak: {}", keycloakId);
        } catch (Exception exception) {
            log.error("Không thể rollback user trên Keycloak: {}", keycloakId);
        }
    }
}
