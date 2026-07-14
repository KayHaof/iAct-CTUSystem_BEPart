package com.example.activityservice.feature.activities.service.impl;

import com.example.activityservice.common.dto.NotificationRequest;
import com.example.activityservice.feature.activities.dto.ActivityStatsResponse;
import com.example.activityservice.feature.activities.dto.DepartmentStatsResponse;
import com.example.activityservice.feature.activities.dto.SystemStatsResponse;
import com.example.activityservice.feature.activities.kafka.ActivityEventProducer;
import com.example.activityservice.feature.activities.model.Activities;
import com.example.activityservice.feature.activities.repository.ActivityRepository;
import com.example.activityservice.feature.activities.service.ActivityCacheService;
import com.example.activityservice.feature.locations.service.ActivityLocationBookingService;
import com.example.activityservice.feature.notification.kafka.NotificationCommandProducer;
import com.example.activityservice.feature.semesters.model.Semesters;
import com.example.activityservice.feature.semesters.repository.SemesterRepository;
import com.example.activityservice.feature.users.model.Users;
import com.example.activityservice.feature.users.repository.UserRepository;
import com.example.activityservice.feature.users.service.LocalDepartmentResolver;
import com.example.event.kafka.KafkaTopics;
import com.example.exception.AppException;
import com.example.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AdminActivityOperations {

    private final ActivityRepository activityRepository;
    private final SemesterRepository semesterRepository;
    private final UserRepository userRepository;
    private final LocalDepartmentResolver localDepartmentResolver;
    private final ActivityEventProducer activityEventProducer;
    private final NotificationCommandProducer notificationCommandProducer;
    private final ActivityCacheService activityCacheService;
    private final ActivityLocationBookingService locationBookingService;

    @Transactional
    public void approveActivity(Long id) {
        Activities activity = getActivityForAction(id);
        if (activity.getStatus() != 0) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Chi duyet duoc hoat dong Cho duyet.");
        }
        Users reviewer = getCurrentReviewer();
        validateApprovalPermission(activity, reviewer);

        activity.setStatus(1);
        activity.setHandledBy(reviewer);
        activity.setHandledAt(LocalDateTime.now());
        Activities savedActivity = activityRepository.save(activity);
        locationBookingService.approveBookingsForActivity(savedActivity.getId(), reviewer);

        activityEventProducer.publishApproved(savedActivity);
        dispatchFacultyRegistrationOpenNotifications(savedActivity);
        activityCacheService.evictActivityListCaches();
    }

    @Transactional
    public void rejectActivity(Long id, String reason) {
        Activities activity = getActivityForAction(id);
        if (activity.getStatus() != 0) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Chi tu choi duoc hoat dong Cho duyet.");
        }
        Users reviewer = getCurrentReviewer();
        validateApprovalPermission(activity, reviewer);

        activity.setStatus(2);
        activity.setReason(reason != null && !reason.isBlank() ? reason : "Khong co ly do");
        activity.setHandledBy(reviewer);
        activity.setHandledAt(LocalDateTime.now());
        Activities savedActivity = activityRepository.save(activity);
        locationBookingService.rejectBookingsForActivity(savedActivity.getId(), reviewer, activity.getReason());

        activityEventProducer.publishRejected(savedActivity);
        activityCacheService.evictActivityListCaches();
    }

    @Transactional
    public void cancelActivity(Long id, String reason) {
        Activities activity = getActivityForAction(id);
        if (activity.getStatus() != 0 && activity.getStatus() != 1) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Loi trang thai");
        }

        activity.setStatus(4);
        activity.setReason(reason != null && !reason.isBlank() ? reason : "Su co ngoai y muon");
        Users reviewer = getCurrentReviewer();
        activity.setHandledBy(reviewer);
        activity.setHandledAt(LocalDateTime.now());
        Activities savedActivity = activityRepository.save(activity);
        locationBookingService.cancelBookingsForActivity(savedActivity.getId(), reviewer, activity.getReason());

        activityEventProducer.publishCancelled(savedActivity);
        activityCacheService.evictActivityListCaches();
    }

    public ActivityStatsResponse getActivityStats() {
        long pending = activityRepository.countByStatus(0);
        long approved = activityRepository.countByStatus(1);
        long rejected = activityRepository.countByStatus(2);

        return ActivityStatsResponse.builder()
                .pendingReview(pending)
                .approvedThisTerm(approved)
                .rejected(rejected)
                .build();
    }

    @Transactional(readOnly = true)
    public DepartmentStatsResponse getDepartmentStatistics(Long departmentId, Long semesterId) {
        Long actualSemesterId = semesterId;
        if (actualSemesterId == null) {
            Semesters semester = semesterRepository.findSemesterByDate(LocalDate.now()).orElse(null);
            if (semester != null) {
                actualSemesterId = semester.getId();
            }
        }

        String departmentName = localDepartmentResolver.resolveDepartmentName(departmentId).orElse(null);
        List<Activities> activities = activityRepository.findByDepartmentId(departmentId);

        int total = activities.size();
        int pending = (int) activities.stream().filter(a -> a.getStatus() == 0).count();
        int approved = (int) activities.stream().filter(a -> a.getStatus() == 1).count();
        int rejected = (int) activities.stream().filter(a -> a.getStatus() == 2).count();
        int cancelled = (int) activities.stream().filter(a -> a.getStatus() == 4).count();

        return DepartmentStatsResponse.builder()
                .departmentId(departmentId)
                .departmentName(departmentName)
                .semesterId(actualSemesterId)
                .totalActivities(total)
                .pendingActivities(pending)
                .approvedActivities(approved)
                .rejectedActivities(rejected)
                .cancelledActivities(cancelled)
                .totalRegistrations(0)
                .totalAttendances(0)
                .totalCancellations(cancelled)
                .attendanceRate(0.0)
                .totalPointsAwarded(0)
                .uniqueStudentsParticipated(0)
                .build();
    }

    @Transactional(readOnly = true)
    public SystemStatsResponse getSystemStatistics(Long semesterId) {
        Long actualSemesterId = semesterId;
        if (actualSemesterId == null) {
            Semesters semester = semesterRepository.findSemesterByDate(LocalDate.now()).orElse(null);
            if (semester != null) {
                actualSemesterId = semester.getId();
            }
        }

        long totalActivities = activityRepository.count();
        long pending = activityRepository.countByStatus(0);
        long approved = activityRepository.countByStatus(1);
        long rejected = activityRepository.countByStatus(2);

        return SystemStatsResponse.builder()
                .semesterId(actualSemesterId)
                .totalActivities((int) totalActivities)
                .pendingApproval((int) pending)
                .approvedThisSemester((int) approved)
                .rejected((int) rejected)
                .approvalRate(totalActivities > 0 ? (approved * 100.0) / totalActivities : 0)
                .totalRegistrations(0L)
                .totalAttendances(0L)
                .averageAttendanceRate(0.0)
                .build();
    }

    private Activities getActivityForAction(Long id) {
        Activities activity = activityRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Khong tim thay!"));
        int currentStatus = activity.getStatus();
        if (currentStatus == 2 || currentStatus == 4) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Da bi Tu choi hoac Huy!");
        }
        return activity;
    }

    private Users getCurrentReviewer() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return userRepository.findByUsername(authentication.getName()).orElse(null);
        }
        return null;
    }

    private void validateApprovalPermission(Activities activity, Users reviewer) {
        if (hasCurrentRole("ROLE_ADMIN")) {
            if (isStudentRepresentativeActivity(activity)) {
                throw new AppException(ErrorCode.FORBIDDEN,
                        "Admin chi duyet hoat dong do Truong/Khoa gui len.");
            }
            return;
        }
        if (hasCurrentRole("ROLE_DEPARTMENT")) {
            if (reviewer == null
                    || reviewer.getDepartmentId() == null
                    || !Objects.equals(reviewer.getDepartmentId(), activity.getDepartmentId())
                    || !isStudentRepresentativeActivity(activity)) {
                throw new AppException(ErrorCode.FORBIDDEN,
                        "Don vi chi duyet hoat dong do dai dien lop/chi doan thuoc khoa/vien cua minh gui len.");
            }
            return;
        }
        throw new AppException(ErrorCode.FORBIDDEN, "Ban khong co quyen duyet hoat dong nay.");
    }

    private boolean isStudentRepresentativeActivity(Activities activity) {
        return activity.getCreatedBy() != null
                && Integer.valueOf(1).equals(activity.getCreatedBy().getRoleType());
    }

    private boolean hasCurrentRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> Objects.equals(authority.getAuthority(), role));
    }

    private void dispatchFacultyRegistrationOpenNotifications(Activities activity) {
        if (!isFacultyInternalActivity(activity) || activity.getDepartmentId() == null || !isRegistrationOpen(activity)) {
            return;
        }

        List<Long> studentIds = userRepository.findActiveStudentIdsByDepartmentId(activity.getDepartmentId());
        for (Long studentId : studentIds) {
            NotificationRequest request = new NotificationRequest();
            request.setUserId(studentId);
            request.setActivityId(activity.getId());
            request.setTitle("Hoat dong da mo dang ky");
            request.setMessage("Hoat dong '" + activity.getTitle()
                    + "' da duoc phe duyet va dang trong thoi gian dang ky.");
            request.setType(1);
            request.setReferenceType("activity-registration-open");
            request.setSourceTopic(KafkaTopics.ACTIVITY_APPROVED);
            request.setSourceEventId("activity-registration-open:" + activity.getId() + ":user:" + studentId);
            notificationCommandProducer.publishCreated(request);
        }
    }

    private boolean isFacultyInternalActivity(Activities activity) {
        return Boolean.TRUE.equals(activity.getIsFaculty()) && !Boolean.TRUE.equals(activity.getIsExternal());
    }

    private boolean isRegistrationOpen(Activities activity) {
        LocalDateTime now = LocalDateTime.now();
        return activity.getRegistrationStart() != null
                && activity.getRegistrationEnd() != null
                && !now.isBefore(activity.getRegistrationStart())
                && !now.isAfter(activity.getRegistrationEnd());
    }
}
