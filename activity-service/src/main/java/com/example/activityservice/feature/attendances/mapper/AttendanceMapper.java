package com.example.activityservice.feature.attendances.mapper;

import com.example.activityservice.feature.attendances.dto.AttendanceResponse;
import com.example.activityservice.feature.attendances.dto.CheckInRequest;
import com.example.activityservice.feature.attendances.model.Attendances;
import com.example.activityservice.feature.registration.model.Registrations;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class AttendanceMapper {

    public Attendances toEntity(CheckInRequest request, Registrations registration) {
        Attendances entity = new Attendances();
        entity.setCheckinTime(LocalDateTime.now());
        entity.setMethod(request.getMethod());
        entity.setLatitude(request.getLatitude());
        entity.setLongitude(request.getLongitude());
        entity.setRegistration(registration);
        return entity;
    }

    public AttendanceResponse toResponse(Attendances entity, String message) {
        if (entity == null) return null;
        return AttendanceResponse.builder()
                .id(entity.getId())
                .registrationId(entity.getRegistration() != null ? entity.getRegistration().getId() : null)
                .checkinTime(entity.getCheckinTime())
                .method(entity.getMethod())
                .message(message)
                .build();
    }
}
