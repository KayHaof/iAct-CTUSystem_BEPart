package com.example.userservice.feature.auth.service;

import com.example.userservice.feature.auth.dto.RegisterRequest;
import com.example.userservice.feature.auth.mapper.AuthMapper;
import com.example.userservice.feature.kafka.UserDomainEventProducer;
import com.example.userservice.feature.user_profile.dto.CreateProfileDto;
import com.example.userservice.feature.user_profile.service.UserProfileService;
import com.example.userservice.feature.users.model.Users;
import com.example.userservice.feature.users.repository.UserRepository;
import com.example.userservice.feature.users.service.UserProjectionPublisher;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    @Test
    void registerUserPublishesProjectionAfterProfileCreation() {
        UserRepository userRepository = mock(UserRepository.class);
        Keycloak keycloak = mock(Keycloak.class);
        WebClient webClient = mock(WebClient.class);
        AuthMapper authMapper = mock(AuthMapper.class);
        UserProfileService userProfileService = mock(UserProfileService.class);
        UserProjectionPublisher userProjectionPublisher = mock(UserProjectionPublisher.class);
        UserDomainEventProducer userDomainEventProducer = mock(UserDomainEventProducer.class);
        AuthService authService = new AuthService(
                userRepository,
                keycloak,
                webClient,
                authMapper,
                userProfileService,
                userProjectionPublisher,
                userDomainEventProducer);

        RegisterRequest request = RegisterRequest.builder()
                .username("sv001")
                .password("Password@123")
                .email("sv001@iact.com")
                .roleType(1)
                .firstName("Nguyen")
                .lastName("An")
                .studentCode("B2100001")
                .classId(10L)
                .build();
        Users mappedUser = Users.builder()
                .username("sv001")
                .email("sv001@iact.com")
                .roleType(1)
                .status(1)
                .build();
        Users savedUser = Users.builder()
                .id(42L)
                .keycloakId("kc-42")
                .username("sv001")
                .email("sv001@iact.com")
                .roleType(1)
                .status(1)
                .build();

        RealmResource realmResource = mock(RealmResource.class);
        UsersResource usersResource = mock(UsersResource.class);
        RolesResource rolesResource = mock(RolesResource.class);
        RoleResource roleResource = mock(RoleResource.class);
        UserResource userResource = mock(UserResource.class);
        RoleMappingResource roleMappingResource = mock(RoleMappingResource.class);
        RoleScopeResource roleScopeResource = mock(RoleScopeResource.class);
        Response response = Response.created(URI.create("http://localhost/admin/realms/myRealm/users/kc-42")).build();

        when(userRepository.existsByUsername("sv001")).thenReturn(false);
        when(userRepository.existsByEmail("sv001@iact.com")).thenReturn(false);
        when(keycloak.realm("myRealm")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(response);
        when(realmResource.roles()).thenReturn(rolesResource);
        when(rolesResource.get("student")).thenReturn(roleResource);
        when(roleResource.toRepresentation()).thenReturn(new RoleRepresentation());
        when(usersResource.get("kc-42")).thenReturn(userResource);
        when(userResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.realmLevel()).thenReturn(roleScopeResource);
        when(authMapper.registerRequestToUser(request)).thenReturn(mappedUser);
        when(userRepository.save(mappedUser)).thenReturn(savedUser);
        when(userRepository.findById(42L)).thenReturn(Optional.of(savedUser));

        authService.registerUser(request);

        verify(userProfileService).createProfile(argThat((CreateProfileDto dto) ->
                dto.getUserId().equals(42L)
                        && dto.getFullName().equals("Nguyen An")
                        && dto.getStudentCode().equals("B2100001")
                        && dto.getClassId().equals(10L)));
        verify(userDomainEventProducer).publishUserCreated(42L);
        verify(userProjectionPublisher).publishById(42L);
    }
}
