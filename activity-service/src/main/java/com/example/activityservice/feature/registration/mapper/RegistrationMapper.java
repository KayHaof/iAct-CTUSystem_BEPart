package com.example.activityservice.feature.registration.mapper;

import com.example.activityservice.feature.activities.model.Activities;
import com.example.activityservice.feature.activitySchedule.model.ActivitySchedule;
import com.example.activityservice.feature.registration.dto.RegistrationResponse;
import com.example.activityservice.feature.registration.model.Registrations;
import com.example.activityservice.feature.users.model.Users;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class RegistrationMapper {

    public RegistrationResponse toResponseWithProof(Registrations entity, Integer proofStatus) {
        if (entity == null) return null;

        RegistrationResponse res = new RegistrationResponse();
        res.setId(entity.getId());
        res.setStudentId(entity.getStudent() != null ? entity.getStudent().getId() : null);
        res.setActivityId(entity.getActivity() != null ? entity.getActivity().getId() : null);
        res.setActivityTitle(entity.getActivity() != null ? entity.getActivity().getTitle() : null);
        res.setRegisteredAt(entity.getRegisteredAt());
        res.setStatus(entity.getStatus());
        res.setCancelReason(entity.getCancelReason());
        res.setIsAttended(entity.getAttendance() != null);
        res.setAttendedAt(entity.getAttendance() != null ? entity.getAttendance().getCheckinTime() : null);
        res.setProofStatus(proofStatus);

        if (entity.getRegisteredSchedules() != null) {
            List<Long> scheduleIds = entity.getRegisteredSchedules().stream()
                    .map(ActivitySchedule::getId)
                    .collect(Collectors.toList());
            res.setScheduleIds(scheduleIds);
        } else {
            res.setScheduleIds(new ArrayList<>());
        }

        return res;
    }

    public RegistrationResponse toResponse(Registrations entity) {
        return toResponseWithProof(entity, 0);
    }

    public Registrations toNewEntity(Users student, Activities activity, List<ActivitySchedule> schedules) {
        Registrations reg = new Registrations();
        reg.setStudent(student);
        reg.setActivity(activity);
        reg.setRegisteredSchedules(schedules != null ? schedules : new ArrayList<>());
        reg.setStatus(0);
        reg.setRegisteredAt(LocalDateTime.now());
        return reg;
    }

    public void reRegisterEntity(Registrations entity, List<ActivitySchedule> schedules) {
        entity.setStatus(0);
        entity.setCancelReason(null);
        entity.setRegisteredAt(LocalDateTime.now());
        entity.setRegisteredSchedules(schedules != null ? schedules : new ArrayList<>());
    }

    public void cancelEntity(Registrations entity, String reason) {
        entity.setStatus(2);
        entity.setCancelReason(reason);
        if (entity.getRegisteredSchedules() != null) {
            entity.getRegisteredSchedules().clear();
        }
    }
}
