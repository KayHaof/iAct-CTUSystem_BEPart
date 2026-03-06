package com.example.feature.activities.mapper;

import com.example.common.dto.BenefitDto;
import com.example.common.dto.NotificationRequest;
import com.example.common.entity.Benefits;
import com.example.common.entity.Semesters;
import com.example.common.entity.Users;
import com.example.common.dto.UserDto;
import com.example.feature.activities.dto.ActivityRequest;
import com.example.feature.activities.dto.ActivityResponse;
import com.example.feature.activities.model.Activities;
import com.example.feature.organizers.dto.OrganizerResponse;
import com.example.feature.organizers.model.Organizers;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ActivityMapper {

    // --- TO ENTITY (CREATE) ---
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "qrCodeToken", expression = "java(java.util.UUID.randomUUID().toString())")

    // Chỉ định rõ lấy từ tham số 'semester' và 'organizer'
    @Mapping(target = "semester", source = "semester")
    @Mapping(target = "organizer", source = "organizer")

    // Đã fix: Chỉ định rõ 'request.' để tránh trùng với 'semester.startDate'
    @Mapping(target = "startDate", source = "request.startDate")
    @Mapping(target = "endDate", source = "request.endDate")
    @Mapping(target = "registrationStart", source = "request.registrationStart")
    @Mapping(target = "registrationEnd", source = "request.registrationEnd")

    @Mapping(target = "benefits", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    Activities toEntity(ActivityRequest request, Semesters semester, Organizers organizer);

    // --- TO RESPONSE ---
    @Mapping(target = "semesterId", source = "semester.id")
    @Mapping(target = "organizer", source = "organizer")
    @Mapping(target = "benefits", source = "benefits")
    @Mapping(target = "createdBy", source = "createdBy")
    ActivityResponse toResponse(Activities entity);

    // --- MAPPING CHO ORGANIZER ---
    // Đã fix: Đổi target từ 'userId' thành 'id' theo gợi ý của trình biên dịch
    @Mapping(target = "id", source = "user.id")
    @Mapping(target = "name", source = "name")
    OrganizerResponse toOrganizerResponse(Organizers organizer);

    // --- CÁC HELPER MAPPING KHÁC ---
    UserDto toUserDto(Users user);

    BenefitDto toBenefitDto(Benefits benefit);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activity", ignore = true)
    @Mapping(target = "category", ignore = true)
    Benefits toBenefitEntity(BenefitDto dto);

    // --- UPDATE ENTITY ---
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "qrCodeToken", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "semester", ignore = true)
    @Mapping(target = "organizer", ignore = true)
    @Mapping(target = "benefits", ignore = true)
    void updateEntityFromRequest(ActivityRequest request, @MappingTarget Activities entity);

    // --- NOTIFICATION ---
    @Mapping(source = "activity.createdBy.id", target = "userId")
    @Mapping(source = "activity.id", target = "activityId")
    NotificationRequest toNotificationRequest(Activities activity, String title, String message, Integer type);
}