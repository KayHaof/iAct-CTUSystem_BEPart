package com.example.userservice.feature.users.mapper;

import com.example.userservice.feature.user_profile.model.DepartmentProfile;
import com.example.userservice.feature.user_profile.model.StudentProfile;
import com.example.userservice.feature.user_profile.dto.ProfileDto;
import com.example.userservice.feature.users.dto.UserResponse;
import com.example.userservice.feature.users.dto.UserSyncDto;
import com.example.userservice.feature.users.model.Users;
import org.mapstruct.*;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    @Mapping(target = "id", source = "user.id")
    @Mapping(target = "status", source = "user.status")
    @Mapping(target = "roleType", source = "user.roleType")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "createdAt", source = "user.createdAt")
    // Đổ toàn bộ các trường từ ProfileDto sang UserResponse
    @Mapping(target = "fullName", source = "profile.fullName")
    @Mapping(target = "studentCode", source = "profile.studentCode")
    @Mapping(target = "avatarUrl", source = "profile.avatarUrl")
    @Mapping(target = "birthday", source = "profile.birthday")
    @Mapping(target = "gender", source = "profile.gender")
    @Mapping(target = "phone", source = "profile.phone")
    @Mapping(target = "address", source = "profile.address")
    @Mapping(target = "departmentId", source = "profile.departmentId")
    @Mapping(target = "departmentName", source = "profile.departmentName")
    @Mapping(target = "classId", source = "profile.classId")
    @Mapping(target = "classCode", source = "profile.classCode")
    UserResponse toResponseAggregated(Users user, ProfileDto profile);

    @Mapping(target = "id", source = "id")
    UserResponse toResponse(Users user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Users toUserFromSyncDto(UserSyncDto dto);

    @Mapping(target = "classId", source = "clazz.id")
    @Mapping(target = "classCode", source = "clazz.classCode")
    @Mapping(target = "className", source = "clazz.name")
    @Mapping(target = "departmentId", ignore = true)
    @Mapping(target = "departmentName", ignore = true)
    ProfileDto toProfileDto(StudentProfile studentProfile);

    @Mapping(target = "departmentId", source = "department.id")
    @Mapping(target = "departmentName", source = "department.name")
    @Mapping(target = "studentCode", ignore = true)
    @Mapping(target = "birthday", ignore = true)
    @Mapping(target = "gender", ignore = true)
    @Mapping(target = "address", ignore = true)
    @Mapping(target = "classId", ignore = true)
    @Mapping(target = "classCode", ignore = true)
    @Mapping(target = "className", ignore = true)
    ProfileDto toProfileDto(DepartmentProfile departmentProfile);
}