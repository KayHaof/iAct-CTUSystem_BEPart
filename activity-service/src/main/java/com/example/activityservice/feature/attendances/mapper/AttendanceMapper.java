package com.example.activityservice.feature.attendances.mapper;

import com.example.activityservice.feature.attendances.dto.AttendanceResponse;
import com.example.activityservice.feature.attendances.model.Attendances;
import org.springframework.stereotype.Component;

@Component
public class AttendanceMapper {

    public AttendanceResponse toResponse(Attendances entity, String message) {
        if (entity == null) return null;
        return AttendanceResponse.builder()
                .id(entity.getId())
                .registrationId(entity.getRegistration() != null ? entity.getRegistration().getId() : null)
                .checkinTime(entity.getCheckinTime())
                .checkoutTime(entity.getCheckoutTime())
                .attendanceStatus(resolveAttendanceStatus(entity))
                .method(entity.getMethod())
                .message(message)
                .build();
    }

    private String resolveAttendanceStatus(Attendances entity) {
        if (entity.getCheckinTime() == null) {
            return "NOT_CHECKED_IN";
        }
        if (entity.getCheckoutTime() != null) {
            return "CHECKED_OUT";
        }
        return "CHECKED_IN";
    }
}
