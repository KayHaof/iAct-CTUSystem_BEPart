package com.example.feature.users.mapper;

import com.example.feature.users.dto.UserResponse;
import com.example.feature.users.dto.UserUpdateRequest;
import com.example.feature.users.model.Users;
import org.mapstruct.*;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface UserProfileMapper {

    @Mapping(target = "classCode", source = "clazz.classCode")
    @Mapping(target = "classId", source = "clazz.id")
    @Mapping(target = "departmentName", source = "department.name")
    @Mapping(target = "departmentId", source = "department.id")
    UserResponse toUserResponse(Users user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "keycloakId", ignore = true)
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "roleType", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateUserFromDto(UserUpdateRequest dto, @MappingTarget Users entity);
}