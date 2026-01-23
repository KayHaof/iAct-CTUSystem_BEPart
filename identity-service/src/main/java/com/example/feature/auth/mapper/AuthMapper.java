package com.example.feature.auth.mapper;

import com.example.feature.auth.dto.RegisterRequest;
import com.example.feature.users.model.Users;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AuthMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "keycloakId", ignore = true)
    @Mapping(target = "status", constant = "1")
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "fullName", expression = "java(request.getFirstName() + \" \" + request.getLastName())")
    @Mapping(target = "clazz.id", source = "classId")
    Users registerRequestToUser(RegisterRequest request);
}
