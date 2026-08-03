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
import java.util.Objects;
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
        res.setIsAttended(Integer.valueOf(1).equals(entity.getStatus()));
        res.setAttendedAt(entity.getAttendance() != null ? entity.getAttendance().getCheckinTime() : null);
        res.setCheckoutAt(entity.getAttendance() != null ? entity.getAttendance().getCheckoutTime() : null);
        res.setProofStatus(proofStatus);
        res.setAttendanceStatus(resolveAttendanceStatus(entity));
        res.setParticipationStatus(resolveParticipationStatus(entity, proofStatus));
        res.setCanSubmitProof(canSubmitProof(entity, proofStatus));
        res.setNextAction(resolveNextAction(res.getParticipationStatus(), res.getCanSubmitProof()));

        if (entity.getRegisteredSchedules() != null) {
            List<Long> scheduleIds = entity.getRegisteredSchedules().stream()
                    .filter(Objects::nonNull)
                    .map(schedule -> schedule.getId())
                    .collect(Collectors.toList());
            res.setScheduleIds(scheduleIds);
        } else {
            res.setScheduleIds(new ArrayList<>());
        }

        return res;
    }

    private String resolveAttendanceStatus(Registrations entity) {
        if (entity.getAttendance() == null || entity.getAttendance().getCheckinTime() == null) {
            return "NOT_CHECKED_IN";
        }
        if (Integer.valueOf(1).equals(entity.getStatus())) {
            return "FACE_VERIFIED";
        }
        if (entity.getAttendance().getCheckoutTime() != null) {
            return "CHECKED_OUT";
        }
        return "CHECKED_IN";
    }

    private String resolveParticipationStatus(Registrations entity, Integer proofStatus) {
        if (entity.getStatus() != null && entity.getStatus() == 2) {
            return "CANCELLED";
        }

        if (entity.getAttendance() == null || entity.getAttendance().getCheckinTime() == null) {
            if (entity.getActivity() != null
                    && entity.getActivity().getEndDate() != null
                    && LocalDateTime.now().isAfter(entity.getActivity().getEndDate())) {
                return "MISSED";
            }
            return "REGISTERED";
        }

        if (entity.getAttendance().getCheckoutTime() == null) {
            return "CHECKED_IN";
        }

        if (!Integer.valueOf(1).equals(entity.getStatus())) {
            return "CHECKED_OUT";
        }

        if (proofStatus != null && proofStatus == 1) {
            return "PROOF_PENDING";
        }
        if (proofStatus != null && proofStatus == 2) {
            return "COMPLETED";
        }
        if (proofStatus != null && proofStatus == 3) {
            return "PROOF_REJECTED";
        }

        return "FACE_VERIFIED";
    }

    private Boolean canSubmitProof(Registrations entity, Integer proofStatus) {
        boolean hasCheckedIn = entity.getAttendance() != null && entity.getAttendance().getCheckinTime() != null;
        boolean hasCheckedOut = entity.getAttendance() != null && entity.getAttendance().getCheckoutTime() != null;
        boolean faceVerified = Integer.valueOf(1).equals(entity.getStatus());
        boolean proofIsOpen = proofStatus == null || proofStatus == 0 || proofStatus == 3;
        return hasCheckedIn && hasCheckedOut && faceVerified && proofIsOpen;
    }

    private String resolveNextAction(String participationStatus, Boolean canSubmitProof) {
        if ("REGISTERED".equals(participationStatus)) {
            return "QR_CHECK_IN";
        }
        if ("CHECKED_IN".equals(participationStatus)) {
            return "QR_CHECK_OUT";
        }
        if ("CHECKED_OUT".equals(participationStatus)) {
            return "FACE_VERIFY";
        }
        if (Boolean.TRUE.equals(canSubmitProof)) {
            return "SUBMIT_PROOF";
        }
        if ("PROOF_PENDING".equals(participationStatus)) {
            return "WAIT_PROOF_REVIEW";
        }
        return "NONE";
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
