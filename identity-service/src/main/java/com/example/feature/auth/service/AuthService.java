package com.example.feature.auth.service;

import com.example.feature.auth.dto.LoginRequest;
import com.example.feature.auth.dto.RegisterRequest;
import com.example.feature.users.model.Users;
import com.example.feature.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.ws.rs.core.Response;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final Keycloak keycloak;
    private final String realm = "myRealm";
    private final WebClient webClient;

    @Value("${app.keycloak.token-uri}")
    private String tokenUrl;

    @Transactional
    public void registerUser(RegisterRequest request) {
        // Define User Representation
        UserRepresentation userRep = new UserRepresentation();
        userRep.setUsername(request.getUsername());
        userRep.setEmail(request.getEmail());
        userRep.setFirstName(request.getFirstName());
        userRep.setLastName(request.getLastName());
        userRep.setEnabled(true);

        userRep.setEmailVerified(false);
        userRep.setRequiredActions(Collections.singletonList("VERIFY_EMAIL"));
//        userRep.setRequiredActions(Collections.emptyList());

        // Mapping Role and Group
        String roleName;
        String groupPath;
        System.out.println(">> Check role = " + request.getRoleType());
        switch (request.getRoleType()) {
            case 2 -> { roleName = "department"; groupPath = "/departments"; }
            case 3 -> { roleName = "admin"; groupPath = "/admins"; }
            case 4 -> { roleName = "other"; groupPath = "/others"; }
            default -> { roleName = "student"; groupPath = "/students"; }
        }

        System.out.println(">> Check role = " + roleName);

        userRep.setGroups(Collections.singletonList(groupPath));

        // Define Password
        CredentialRepresentation cred = new CredentialRepresentation();
        cred.setType(CredentialRepresentation.PASSWORD);
        cred.setValue(request.getPassword());
        cred.setTemporary(false);
        userRep.setCredentials(Collections.singletonList(cred));

        try {
            Response response = keycloak.realm(realm).users().create(userRep);
            System.out.println(">>>Check status = " + response.getStatus());

            if (response.getStatus() == 201) {
                String path = response.getLocation().getPath();
                String keycloakId = path.substring(path.lastIndexOf("/") + 1);

                assignRoleToUser(keycloakId, roleName);

                keycloak.realm(realm).users().get(keycloakId)
                        .executeActionsEmail(Collections.singletonList("VERIFY_EMAIL"));

                Users user = Users.builder()
                        .keycloakId(keycloakId)
                        .username(request.getUsername())
                        .email(request.getEmail())
                        .roleType(request.getRoleType())
                        .status(1)
                        .build();
                userRepository.save(user);
            } else if (response.getStatus() == 409) {
                throw new RuntimeException("Username hoặc Email đã tồn tại trên Keycloak!");
            } else {
                throw new RuntimeException("Lỗi Keycloak: " + response.getStatus());
            }
        } catch (Exception e) {
            throw new RuntimeException("Quá trình đăng ký thất bại: " + e.getMessage());
        }
    }

    private void assignRoleToUser(String userId, String roleName) {
        var roleRep = keycloak.realm(realm).roles().get(roleName).toRepresentation();
        keycloak.realm(realm).users().get(userId).roles().realmLevel().add(Collections.singletonList(roleRep));
    }

    public Object loginUser(LoginRequest request) {
        System.out.println(">>> Check logining = " + request.toString());
        return webClient.post()
                .uri(tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("grant_type", "password")
                        .with("client_id", "myclient123@")
                        .with("client_secret", "FNOI5Vml17alkhtIBNFCe4IafsJtXMw2")
                        .with("username", request.getUsername())
                        .with("password", request.getPassword()))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response ->
                        response.bodyToMono(String.class).flatMap(error ->
                                Mono.error(new RuntimeException("Đăng nhập thất bại: " + error))))
                .bodyToMono(Object.class)
                .block();
    }
}