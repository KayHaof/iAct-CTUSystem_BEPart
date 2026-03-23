package com.example.feature.users.mapper;

import com.example.feature.users.dto.ProfileDto;
import com.example.feature.users.dto.UserResponse;
import com.example.feature.users.dto.UserSyncDto;
import com.example.feature.users.model.Users;
import org.mapstruct.*;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface UserProfileMapper {
    // 1. HÀM TRỘN DỮ LIỆU (AGGREGATOR) TỪ 2 SERVICE
    @Mapping(target = "id", source = "user.id")
    @Mapping(target = "status", source = "user.status")
    @Mapping(target = "roleType", source = "user.roleType")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "createdAt", source = "user.createdAt")
    @Mapping(target = "departmentId", source = "profile.departmentId")
    @Mapping(target = "departmentName", source = "profile.departmentName")
    @Mapping(target = "classId", source = "profile.classId")
    @Mapping(target = "classCode", source = "profile.classCode")
    UserResponse toResponseAggregated(Users user, ProfileDto profile);

    // 2. HÀM DỰ PHÒNG (Khi Profile-Service bị lỗi hoặc chưa kịp tạo profile)
    @Mapping(target = "id", source = "id")
    UserResponse toResponse(Users user);

    // 3. HÀM SYNC TỪ KEYCLOAK VỀ LOCAL (Bảng Lõi)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Users toUserFromSyncDto(UserSyncDto dto);
}