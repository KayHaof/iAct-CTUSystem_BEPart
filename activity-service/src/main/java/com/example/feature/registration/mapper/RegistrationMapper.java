package com.example.feature.registration.mapper;

import com.example.feature.registration.dto.RegistrationResponse;
import com.example.feature.registration.model.Registrations;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RegistrationMapper {

    @Mapping(target = "studentId", source = "student.id")
    @Mapping(target = "studentName", source = "student.fullName")
    @Mapping(target = "avatarUrl", source = "student.avatarUrl")
    @Mapping(target = "studentCode", source = "student.studentCode")
    @Mapping(target = "activityId", source = "activity.id")
    @Mapping(target = "activityTitle", source = "activity.title")
    @Mapping(target = "isAttended", expression = "java(entity.getAttendance() != null)")
    @Mapping(target = "attendedAt", source = "attendance.checkinTime")
    @Mapping(target = "scheduleIds", expression = "java( entity.getRegisteredSchedules().stream().map(s -> s.getId()).collect(java.util.stream.Collectors.toList()) )")
    RegistrationResponse toResponse(Registrations entity);
}