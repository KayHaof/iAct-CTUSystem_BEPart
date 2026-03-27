package com.example.feature.activities.mapper;

import com.example.common.dto.BenefitDto;
import com.example.common.dto.NotificationRequest;
import com.example.common.entity.Benefits;
import com.example.common.entity.Semesters;
import com.example.common.entity.Users;
import com.example.common.dto.UserDto;
import com.example.dto.PageDTO;
import com.example.feature.activitySchedule.dto.ActivityScheduleDto;
import com.example.feature.activitySchedule.model.ActivitySchedule;
import com.example.feature.activities.dto.ActivityRequest;
import com.example.feature.activities.dto.ActivityResponse;
import com.example.feature.activities.model.Activities;
import com.example.feature.organizers.dto.OrganizerResponse;
import com.example.feature.organizers.model.Organizers;
import org.mapstruct.*;
import org.springframework.data.domain.Page;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ActivityMapper {
    // --- TO ENTITY (CREATE) ---
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "qrCodeToken", expression = "java(java.util.UUID.randomUUID().toString())")
    @Mapping(target = "semester", source = "semester")
    @Mapping(target = "organizer", source = "organizer")
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
    @Mapping(target = "departmentId", source = "organizer.department.id")
    @Mapping(target = "departmentName", source = "organizer.department.name")
    @Mapping(target = "schedules", source = "schedules")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "reason", source = "reason")
    @Mapping(target = "handledBy", source = "handledBy")
    @Mapping(target = "handledAt", source = "handledAt")
    ActivityResponse toResponse(Activities entity);

    // --- MAPPING CHO ORGANIZER ---
    @Mapping(target = "id", source = "user.id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "departmentId", source = "department.id")
    OrganizerResponse toOrganizerResponse(Organizers organizer);

    // --- CÁC HELPER MAPPING KHÁC ---
    UserDto toUserDto(Users user);

    @Mapping(source = "category.id", target = "categoryId")
    BenefitDto toBenefitDto(Benefits benefit);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activity", ignore = true)
    @Mapping(target = "category", ignore = true)
    Benefits toBenefitEntity(BenefitDto dto);

    ActivityScheduleDto toScheduleDto(ActivitySchedule schedule);
    List<ActivityScheduleDto> toScheduleDtoList(List<ActivitySchedule> schedules);

    // --- UPDATE ENTITY ---
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "qrCodeToken", ignore = true)
    @Mapping(target = "semester", ignore = true)
    @Mapping(target = "organizer", ignore = true)
    @Mapping(target = "benefits", ignore = true)
    @Mapping(target = "schedules", ignore = true)
    void updateEntityFromRequest(ActivityRequest request, @MappingTarget Activities entity);

    // --- NOTIFICATION ---
    @Mapping(source = "activity.createdBy.id", target = "userId")
    @Mapping(source = "activity.id", target = "activityId")
    NotificationRequest toNotificationRequest(Activities activity, String title, String message, Integer type);

    default PageDTO<ActivityResponse> toPageDTO(Page<Activities> page, List<ActivityResponse> dtoList) {
        if (page == null) {
            return null;
        }

        PageDTO<ActivityResponse> result = new PageDTO<>();
        result.setPageNumber(page.getNumber() + 1);
        result.setPageSize(page.getSize());
        result.setTotalPage(page.getTotalPages());
        result.setTotalRows(page.getTotalElements());
        result.setLast(page.isLast());
        result.setData(dtoList);

        return result;
    }

    @AfterMapping
    default void linkSchedulesToActivity(@MappingTarget Activities activity) {
        if (activity.getSchedules() != null) {
            for (ActivitySchedule schedule : activity.getSchedules()) {
                schedule.setActivity(activity);
            }
        }
    }
}