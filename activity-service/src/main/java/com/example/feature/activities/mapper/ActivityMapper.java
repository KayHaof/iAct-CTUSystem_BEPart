package com.example.feature.activities.mapper;

import com.example.common.dto.BenefitDto;
import com.example.common.entity.Benefits;
import com.example.common.entity.Semesters;
import com.example.common.entity.Users;
import com.example.common.dto.UserDto;
import com.example.feature.activities.dto.ActivityRequest;
import com.example.feature.activities.dto.ActivityResponse;
import com.example.feature.activities.model.Activities;
import com.example.feature.organizers.model.Organizers;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ActivityMapper {

    // --- TO ENTITY (CREATE) ---
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", constant = "0")
    @Mapping(target = "qrCodeToken", expression = "java(java.util.UUID.randomUUID().toString())")

    @Mapping(target = "semester", source = "semester")
    @Mapping(target = "organizer", source = "organizer")

    @Mapping(target = "title", source = "request.title")
    @Mapping(target = "description", source = "request.description")
    @Mapping(target = "content", source = "request.content")
    @Mapping(target = "location", source = "request.location")
    @Mapping(target = "maxParticipants", source = "request.maxParticipants")
    @Mapping(target = "coverImage", source = "request.coverImage")
    @Mapping(target = "thumbnail", source = "request.thumbnail")
    @Mapping(target = "sourceLink", source = "request.sourceLink")
    @Mapping(target = "isExternal", source = "request.isExternal")

    @Mapping(target = "startDate", source = "request.startDate")
    @Mapping(target = "endDate", source = "request.endDate")
    @Mapping(target = "registrationStart", source = "request.registrationStart")
    @Mapping(target = "registrationEnd", source = "request.registrationEnd")

    @Mapping(target = "benefits", ignore = true)
    Activities toEntity(ActivityRequest request, Semesters semester, Organizers organizer);

    // --- HELPER ---
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activity", ignore = true)
    @Mapping(target = "category", ignore = true)
    Benefits toBenefitEntity(BenefitDto dto);

    // --- TO RESPONSE ---
    @Mapping(target = "semesterId", source = "semester.id")
    @Mapping(target = "organizerId", source = "organizer.id")
    @Mapping(target = "benefits", source = "benefits")
    @Mapping(target = "createdBy", source = "createdBy")
    ActivityResponse toResponse(Activities entity);

    UserDto toUserDto(Users user);

    BenefitDto toBenefitDto(Benefits benefit);

    // --- UPDATE ENTITY TỪ REQUEST ---
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "qrCodeToken", ignore = true)
    @Mapping(target = "status", ignore = true)

    @Mapping(target = "semester", ignore = true)
    @Mapping(target = "organizer", ignore = true)
    @Mapping(target = "benefits", ignore = true)
    void updateEntityFromRequest(ActivityRequest request, @MappingTarget Activities entity);
}