package com.example.activityservice.feature.registration.mapper;

import com.example.activityservice.feature.activities.model.Activities;
import com.example.activityservice.feature.activitySchedule.model.ActivitySchedule;
import com.example.activityservice.feature.attendances.dto.AttendanceResponse;
import com.example.activityservice.feature.attendances.mapper.AttendanceMapper;
import com.example.activityservice.feature.attendances.model.Attendances;
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

    private final AttendanceMapper attendanceMapper = new AttendanceMapper();

    public RegistrationResponse toResponseWithProof(Registrations entity, Integer proofStatus) {
        if (entity == null) return null;

        RegistrationResponse res = new RegistrationResponse();
        res.setId(entity.getId());
        res.setStudentId(entity.getStudent() != null ? entity.getStudent().getId() : null);
        res.setActivityId(entity.getActivity() != null ? entity.getActivity().getId() : null);
        res.setActivityTitle(entity.getActivity() != null ? entity.getActivity().getTitle() : null);
        res.setRegisteredAt(entity.getRegisteredAt());
        List<Attendances> attendances = entity.getAttendances() != null ? entity.getAttendances() : List.of();
        Attendances representativeAttendance = resolveRepresentativeAttendance(entity, attendances);

        res.setStatus(entity.getStatus());
        res.setCancelReason(entity.getCancelReason());
        res.setAbsenceReason(entity.getAbsenceReason());
        res.setAbsenceReviewed(entity.getAbsenceReviewed());
        res.setAbsenceReviewedBy(entity.getAbsenceReviewedBy());
        res.setAbsenceReviewedAt(entity.getAbsenceReviewedAt());
        res.setAbsenceReviewNote(entity.getAbsenceReviewNote());
        res.setIsAttended(hasVerifiedAllRegisteredSessions(entity, attendances));
        res.setAttendedAt(representativeAttendance != null ? representativeAttendance.getCheckinTime() : null);
        res.setCheckoutAt(representativeAttendance != null ? representativeAttendance.getCheckoutTime() : null);
        res.setProofStatus(proofStatus);
        res.setAttendanceStatus(resolveAttendanceStatus(entity, attendances));
        res.setParticipationStatus(resolveParticipationStatus(entity, attendances, proofStatus));
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
        res.setAttendanceSessions(toAttendanceSessions(attendances));
        res.setRegisteredSessionCount(res.getScheduleIds() != null ? res.getScheduleIds().size() : 0);
        res.setFaceVerifiedSessionCount((int) attendances.stream()
                .filter(attendance -> Integer.valueOf(Attendances.STATUS_FACE_VERIFIED).equals(attendance.getStatus()))
                .count());
        res.setAbsentSessionCount((int) attendances.stream()
                .filter(attendance -> Integer.valueOf(Attendances.STATUS_ABSENT).equals(attendance.getStatus()))
                .count());

        return res;
    }

    private String resolveAttendanceStatus(Registrations entity, List<Attendances> attendances) {
        if (Integer.valueOf(Registrations.STATUS_ABSENT).equals(entity.getStatus())) {
            return "ABSENT";
        }
        if (hasVerifiedAllRegisteredSessions(entity, attendances)) {
            return "FACE_VERIFIED";
        }
        if (hasStatus(attendances, Attendances.STATUS_ABSENT)) {
            return "ABSENT";
        }
        if (hasStatus(attendances, Attendances.STATUS_CHECKED_OUT)) {
            return "CHECKED_OUT";
        }
        if (hasStatus(attendances, Attendances.STATUS_CHECKED_IN)) {
            return "CHECKED_IN";
        }
        Attendances representativeAttendance = resolveRepresentativeAttendance(entity, attendances);
        if (representativeAttendance == null || representativeAttendance.getCheckinTime() == null) {
            return "NOT_CHECKED_IN";
        }
        if (representativeAttendance.getCheckoutTime() != null) {
            return "CHECKED_OUT";
        }
        return "CHECKED_IN";
    }

    private String resolveParticipationStatus(Registrations entity, List<Attendances> attendances, Integer proofStatus) {
        if (entity.getStatus() != null && entity.getStatus() == 2) {
            return "CANCELLED";
        }

        if (Integer.valueOf(Registrations.STATUS_ABSENT).equals(entity.getStatus())) {
            return "ABSENT";
        }

        if (hasVerifiedAllRegisteredSessions(entity, attendances)) {
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

        if (hasStatus(attendances, Attendances.STATUS_ABSENT)) {
            return "MISSED";
        }

        Attendances representativeAttendance = resolveRepresentativeAttendance(entity, attendances);
        if (representativeAttendance == null || representativeAttendance.getCheckinTime() == null) {
            if (entity.getActivity() != null
                    && entity.getActivity().getEndDate() != null
                    && LocalDateTime.now().isAfter(entity.getActivity().getEndDate())) {
                return "MISSED";
            }
            return "REGISTERED";
        }

        if (representativeAttendance.getCheckoutTime() == null) {
            return "CHECKED_IN";
        }

        return "CHECKED_OUT";
    }

    private Boolean canSubmitProof(Registrations entity, Integer proofStatus) {
        List<Attendances> attendances = entity.getAttendances() != null ? entity.getAttendances() : List.of();
        boolean faceVerified = !Integer.valueOf(Registrations.STATUS_ABSENT).equals(entity.getStatus())
                && hasVerifiedAllRegisteredSessions(entity, attendances);
        boolean proofIsOpen = proofStatus == null || proofStatus == 0 || proofStatus == 3;
        return faceVerified && proofIsOpen;
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

    private List<AttendanceResponse> toAttendanceSessions(List<Attendances> attendances) {
        return attendances.stream()
                .filter(Objects::nonNull)
                .map(attendance -> attendanceMapper.toResponse(attendance, null))
                .collect(Collectors.toList());
    }

    private Attendances resolveRepresentativeAttendance(Registrations entity, List<Attendances> attendances) {
        if (attendances == null || attendances.isEmpty()) {
            return entity.getAttendance();
        }
        return attendances.stream()
                .filter(Objects::nonNull)
                .filter(attendance -> attendance.getCheckinTime() != null || attendance.getCheckoutTime() != null)
                .findFirst()
                .orElse(attendances.get(0));
    }

    private boolean hasVerifiedAllRegisteredSessions(Registrations entity, List<Attendances> attendances) {
        List<ActivitySchedule> registeredSchedules = entity.getRegisteredSchedules() != null
                ? entity.getRegisteredSchedules()
                : List.of();

        if (registeredSchedules.isEmpty()) {
            return attendances.stream()
                    .anyMatch(attendance -> attendance.getSchedule() == null
                            && Integer.valueOf(Attendances.STATUS_FACE_VERIFIED).equals(attendance.getStatus()))
                    || Integer.valueOf(1).equals(entity.getStatus());
        }

        List<Long> verifiedScheduleIds = attendances.stream()
                .filter(attendance -> Integer.valueOf(Attendances.STATUS_FACE_VERIFIED).equals(attendance.getStatus()))
                .map(attendance -> attendance.getSchedule())
                .filter(Objects::nonNull)
                .map(schedule -> schedule.getId())
                .collect(Collectors.toList());

        return registeredSchedules.stream()
                .filter(Objects::nonNull)
                .map(schedule -> schedule.getId())
                .allMatch(verifiedScheduleIds::contains);
    }

    private boolean hasStatus(List<Attendances> attendances, Integer status) {
        return attendances.stream()
                .anyMatch(attendance -> Integer.valueOf(status).equals(attendance.getStatus()));
    }

    public RegistrationResponse toResponse(Registrations entity) {
        return toResponseWithProof(entity, 0);
    }

    public Registrations toNewEntity(Users student, Activities activity, List<ActivitySchedule> schedules) {
        Registrations reg = new Registrations();
        reg.setStudent(student);
        reg.setActivity(activity);
        reg.setRegisteredSchedules(schedules != null ? schedules : new ArrayList<>());
        reg.setStatus(Registrations.STATUS_REGISTERED);
        reg.setRegisteredAt(LocalDateTime.now());
        return reg;
    }

    public void reRegisterEntity(Registrations entity, List<ActivitySchedule> schedules) {
        entity.setStatus(Registrations.STATUS_REGISTERED);
        entity.setCancelReason(null);
        entity.setAbsenceReason(null);
        entity.setAbsenceReviewed(false);
        entity.setAbsenceReviewedBy(null);
        entity.setAbsenceReviewedAt(null);
        entity.setAbsenceReviewNote(null);
        entity.setRegisteredAt(LocalDateTime.now());
        entity.setRegisteredSchedules(schedules != null ? schedules : new ArrayList<>());
    }

    public void cancelEntity(Registrations entity, String reason) {
        entity.setStatus(Registrations.STATUS_CANCELLED);
        entity.setCancelReason(reason);
        if (entity.getRegisteredSchedules() != null) {
            entity.getRegisteredSchedules().clear();
        }
    }
}
