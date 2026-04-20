package com.example.feature.activities.mapper;

import com.example.common.dto.NotificationRequest;
import com.example.dto.PageDTO;
import com.example.feature.activities.dto.ActivityTimeLocationResponse;
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
    @Mapping(target = "organizer", source = "organizer")
    @Mapping(target = "startDate", source = "request.startDate")
    @Mapping(target = "endDate", source = "request.endDate")
    @Mapping(target = "registrationStart", source = "request.registrationStart")
    @Mapping(target = "registrationEnd", source = "request.registrationEnd")
    // Map trực tiếp ID thay vì truyền Object Semesters
    @Mapping(target = "semesterId", source = "request.semesterId")
    @Mapping(target = "createdBy", ignore = true) // Sẽ được set bằng Long ID trong tầng Service
    Activities toEntity(ActivityRequest request, Organizers organizer);

    // --- TO RESPONSE ---
    @Mapping(target = "semesterId", source = "semesterId")
    @Mapping(target = "organizer", source = "organizer")
    @Mapping(target = "schedules", source = "schedules")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "reason", source = "reason")
    @Mapping(target = "handledAt", source = "handledAt")

    // BẮT BUỘC PHẢI THÊM CÁC DÒNG IGNORE NÀY ĐỂ HẾT BÁO ĐỎ:
    @Mapping(target = "createdBy", ignore = true) // Sẽ gọi API lấy UserDto đắp vào sau
    @Mapping(target = "handledBy", ignore = true) // Sẽ gọi API lấy UserDto đắp vào sau
    @Mapping(target = "benefits", ignore = true)  // Sẽ gọi API lấy BenefitDto đắp vào sau
    @Mapping(target = "departmentId", ignore = true)
    @Mapping(target = "departmentName", ignore = true)
    ActivityResponse toResponse(Activities entity);

    // --- LẤY THỜI GIAN HOẠT ĐỘNG ---
    ActivityTimeLocationResponse toTimeResponse(Activities entity);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "departmentId", source = "departmentId")
    OrganizerResponse toOrganizerResponse(Organizers organizer);

    ActivityScheduleDto toScheduleDto(ActivitySchedule schedule);
    List<ActivityScheduleDto> toScheduleDtoList(List<ActivitySchedule> schedules);

    // --- UPDATE ENTITY ---
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "qrCodeToken", ignore = true)
    @Mapping(target = "organizer", ignore = true)
    @Mapping(target = "schedules", ignore = true)
    void updateEntityFromRequest(ActivityRequest request, @MappingTarget Activities entity);

    // --- NOTIFICATION ---
    // source đổi từ "activity.createdBy.id" thành "activity.createdBy" (vì nó vốn là ID rồi)
    @Mapping(source = "activity.createdBy", target = "userId")
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