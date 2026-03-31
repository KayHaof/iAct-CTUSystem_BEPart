package com.example.feature.attendances.mapper;

import com.example.feature.attendances.dto.AttendanceResponse;
import com.example.feature.attendances.dto.CheckInRequest;
import com.example.feature.attendances.model.Attendances;
import com.example.feature.registration.model.Registrations;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AttendanceMapper {

    // 1. Chuyển Request + Registration thành Entity
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "checkoutTime", ignore = true)
    @Mapping(target = "registration", source = "registration")
    @Mapping(target = "checkinTime", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "method", source = "request.method")
    @Mapping(target = "latitude", source = "request.latitude")
    @Mapping(target = "longitude", source = "request.longitude")
    Attendances toEntity(CheckInRequest request, Registrations registration);

    // 2. Chuyển Entity thành Response
    @Mapping(target = "registrationId", source = "entity.registration.id")
    @Mapping(target = "message", source = "message")
    AttendanceResponse toResponse(Attendances entity, String message);
}
