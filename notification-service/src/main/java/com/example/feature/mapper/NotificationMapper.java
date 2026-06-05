package com.example.feature.mapper;

import com.example.feature.dto.NotificationRequest;
import com.example.feature.dto.NotificationResponse;
import com.example.feature.model.Notifications;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface NotificationMapper {
    // 1. REQUEST -> ENTITY
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isRead", constant = "false")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(source = "userId", target = "userId")
    @Mapping(source = "activityId", target = "activityId")
    @Mapping(source = "message", target = "message")
    @Mapping(source = "title", target = "title")
    @Mapping(source = "type", target = "type")
    Notifications toEntity(NotificationRequest request);

    // 2. ENTITY -> RESPONSE
    @Mapping(source = "id", target = "id")
    @Mapping(source = "title", target = "title")
    @Mapping(source = "message", target = "message")
    @Mapping(source = "type", target = "type")
    @Mapping(source = "isRead", target = "isRead")
    @Mapping(source = "createdAt", target = "createdAt")
    @Mapping(source = "activityId", target = "activityId")
    NotificationResponse toResponse(Notifications entity);
}