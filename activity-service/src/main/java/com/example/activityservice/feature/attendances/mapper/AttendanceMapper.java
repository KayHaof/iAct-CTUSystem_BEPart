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
                .scheduleId(entity.getSchedule() != null ? entity.getSchedule().getId() : null)
                .scheduleTitle(entity.getSchedule() != null ? entity.getSchedule().getTitle() : null)
                .scheduleStartTime(entity.getSchedule() != null ? entity.getSchedule().getStartTime() : null)
                .scheduleEndTime(entity.getSchedule() != null ? entity.getSchedule().getEndTime() : null)
                .checkinTime(entity.getCheckinTime())
                .checkoutTime(entity.getCheckoutTime())
                .attendanceStatus(resolveAttendanceStatus(entity))
                .status(entity.getStatus())
                .method(entity.getMethod())
                .message(message)
                .build();
    }

    private String resolveAttendanceStatus(Attendances entity) {
        if (Integer.valueOf(Attendances.STATUS_ABSENT).equals(entity.getStatus())) {
            return "ABSENT";
        }
        if (Integer.valueOf(Attendances.STATUS_FACE_VERIFIED).equals(entity.getStatus())) {
            return "FACE_VERIFIED";
        }
        if (entity.getCheckinTime() == null) {
            return "NOT_CHECKED_IN";
        }
        if (entity.getCheckoutTime() != null) {
            return "CHECKED_OUT";
        }
        return "CHECKED_IN";
    }
}
