package com.example.feature.mapper;

import com.example.common.entity.Activities;
import com.example.entity.Users;
import com.example.feature.dto.NotificationRequest;
import com.example.feature.dto.NotificationResponse;
import com.example.feature.model.Notifications;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface NotificationMapper {
    // 1. REQUEST -> ENTITY

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isRead", constant = "false")
    @Mapping(target = "createdAt", ignore = true)

    @Mapping(source = "message", target = "message")
    @Mapping(source = "title", target = "title")
    @Mapping(source = "type", target = "type")

    @Mapping(source = "userId", target = "user", qualifiedByName = "mapUserIdToUser")
    @Mapping(source = "activityId", target = "activity", qualifiedByName = "mapActivityIdToActivity")
    Notifications toEntity(NotificationRequest request);

    // 2. ENTITY -> RESPONSE
    @Mapping(source = "id", target = "id")
    @Mapping(source = "title", target = "title")
    @Mapping(source = "message", target = "message")
    @Mapping(source = "type", target = "type")
    @Mapping(source = "isRead", target = "isRead")
    @Mapping(source = "createdAt", target = "createdAt")

    // Flatten data từ Activity sang
    @Mapping(source = "activity.id", target = "activityId")
    @Mapping(source = "activity.title", target = "activityTitle")
    @Mapping(source = "activity.thumbnail", target = "activityThumbnail")
    NotificationResponse toResponse(Notifications entity);

    // 3. HELPER METHODS
    @Named("mapUserIdToUser")
    default Users mapUserIdToUser(Long userId) {
        if (userId == null) {
            return null;
        }
        Users user = new Users();
        user.setId(userId);
        return user;
    }

    @Named("mapActivityIdToActivity")
    default Activities mapActivityIdToActivity(Long activityId) {
        if (activityId == null) {
            return null;
        }
        Activities activity = new Activities();
        activity.setId(activityId);
        return activity;
    }
}