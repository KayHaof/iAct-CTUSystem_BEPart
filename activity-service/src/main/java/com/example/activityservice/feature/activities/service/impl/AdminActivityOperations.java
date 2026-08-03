package com.example.activityservice.feature.activities.service.impl;

import com.example.activityservice.feature.activities.dto.ActivityStatsResponse;
import com.example.activityservice.feature.activities.dto.DepartmentActivityStatsResponse;
import com.example.activityservice.feature.activities.dto.DepartmentStatsResponse;
import com.example.activityservice.feature.activities.dto.SystemStatsResponse;
import com.example.activityservice.feature.activities.kafka.ActivityEventProducer;
import com.example.activityservice.feature.activities.model.Activities;
import com.example.activityservice.feature.activities.repository.ActivityRepository;
import com.example.activityservice.feature.activities.specification.ActivitySpecification;
import com.example.activityservice.feature.activities.service.ActivityCacheService;
import com.example.activityservice.feature.activities.service.ActivityRegistrationNotificationService;
import com.example.activityservice.feature.locations.service.ActivityLocationBookingService;
import com.example.activityservice.feature.semesters.model.Semesters;
import com.example.activityservice.feature.semesters.repository.SemesterRepository;
import com.example.activityservice.feature.users.model.Users;
import com.example.activityservice.feature.users.repository.UserRepository;
import com.example.activityservice.feature.users.service.LocalDepartmentResolver;
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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AdminActivityOperations {
    private static final int ROLE_DEPARTMENT = 2;
    private static final int STATUS_PENDING = 0;
    private static final int STATUS_APPROVED = 1;
    private static final int STATUS_REJECTED = 2;
    private static final List<Integer> MODERATION_STATUSES = List.of(
            STATUS_PENDING,
            STATUS_APPROVED,
            STATUS_REJECTED
    );

    private final ActivityRepository activityRepository;
    private final SemesterRepository semesterRepository;
    private final UserRepository userRepository;
    private final LocalDepartmentResolver localDepartmentResolver;
    private final ActivityEventProducer activityEventProducer;
    private final ActivityCacheService activityCacheService;
    private final ActivityLocationBookingService locationBookingService;
    private final ActivityRegistrationNotificationService registrationNotificationService;
    private final ActivityAccessSupport accessSupport;

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
        registrationNotificationService.notifyIfRegistrationOpen(savedActivity);
        activityCacheService.evictActivityListCaches();
    }

    @Transactional
    public void rejectActivity(Long id, String reason) {
        Activities activity = getActivityForAction(id);
        if (activity.getStatus() != 0) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Chi tu choi duoc hoat dong Cho duyet.");
        }
        if (reason == null || reason.isBlank()) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Vui long nhap ly do tu choi cu the.");
        }
        Users reviewer = getCurrentReviewer();
        validateApprovalPermission(activity, reviewer);

        String rejectionReason = reason.trim();
        activity.setStatus(2);
        activity.setReason(rejectionReason);
        activity.setHandledBy(reviewer);
        activity.setHandledAt(LocalDateTime.now());
        Activities savedActivity = activityRepository.save(activity);
        locationBookingService.rejectBookingsForActivity(savedActivity.getId(), reviewer, rejectionReason);

        activityEventProducer.publishRejected(savedActivity);
        activityCacheService.evictActivityListCaches();
    }

    @Transactional
    public void cancelActivity(Long id, String reason) {
        Activities activity = getActivityForAction(id);
        if (activity.getStatus() != 0 && activity.getStatus() != 1) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Loi trang thai");
        }
        if (!isDepartmentSubmittedActivity(activity) || !Boolean.TRUE.equals(activity.getRequiresAdminApproval())) {
            throw new AppException(ErrorCode.FORBIDDEN,
                    "Admin chi huy hoat dong do Khoa/Don vi gui len.");
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
        var baseSpec = moderationScope();
        long pending = activityRepository.count(baseSpec.and(ActivitySpecification.hasStatusIn(List.of(STATUS_PENDING))));
        long approved = activityRepository.count(baseSpec.and(ActivitySpecification.hasStatusIn(List.of(STATUS_APPROVED))));
        long rejected = activityRepository.count(baseSpec.and(ActivitySpecification.hasStatusIn(List.of(STATUS_REJECTED))));

        return ActivityStatsResponse.builder()
                .pendingReview(pending)
                .approvedThisTerm(approved)
                .rejected(rejected)
                .byDepartment(buildDepartmentActivityStats())
                .build();
    }

    @Transactional(readOnly = true)
    public DepartmentStatsResponse getDepartmentStatistics(Long departmentId, Long semesterId) {
        Long scopedDepartmentId = resolveDepartmentStatisticsScope(departmentId);
        Long actualSemesterId = semesterId;
        if (actualSemesterId == null) {
            Semesters semester = semesterRepository.findSemesterByDate(LocalDate.now()).orElse(null);
            if (semester != null) {
                actualSemesterId = semester.getId();
            }
        }

        String departmentName = localDepartmentResolver.resolveDepartmentName(scopedDepartmentId).orElse(null);
        List<Activities> activities = activityRepository.findByDepartmentId(scopedDepartmentId);

        int total = activities.size();
        int pending = (int) activities.stream().filter(a -> a.getStatus() == 0).count();
        int approved = (int) activities.stream().filter(a -> a.getStatus() == 1).count();
        int rejected = (int) activities.stream().filter(a -> a.getStatus() == 2).count();
        int cancelled = (int) activities.stream().filter(a -> a.getStatus() == 4).count();

        return DepartmentStatsResponse.builder()
                .departmentId(scopedDepartmentId)
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

    private Long resolveDepartmentStatisticsScope(Long requestedDepartmentId) {
        if (accessSupport.isCurrentDepartment()) {
            return accessSupport.requireCurrentDepartmentId();
        }
        if (accessSupport.isCurrentAdmin()) {
            return requestedDepartmentId;
        }
        throw new AppException(ErrorCode.FORBIDDEN, "Ban khong co quyen xem thong ke don vi.");
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

        var scope = ActivitySpecification.isNotDepartmentDirectActivity();
        long totalActivities = activityRepository.count(scope);
        long pending = activityRepository.count(scope.and(ActivitySpecification.hasStatusIn(List.of(0))));
        long approved = activityRepository.count(scope.and(ActivitySpecification.hasStatusIn(List.of(1))));
        long rejected = activityRepository.count(scope.and(ActivitySpecification.hasStatusIn(List.of(2))));

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
            if (!isDepartmentSubmittedActivity(activity) || !Boolean.TRUE.equals(activity.getRequiresAdminApproval())) {
                throw new AppException(ErrorCode.FORBIDDEN,
                        "Admin chi duyet hoat dong do Khoa/Don vi gui len.");
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

    private List<DepartmentActivityStatsResponse> buildDepartmentActivityStats() {
        List<ActivityRepository.DepartmentStatusCountProjection> rows =
                activityRepository.countByCreatorRoleTypeAndRequiresAdminApprovalAndStatusesGroupedByDepartment(
                        ROLE_DEPARTMENT,
                        true,
                        MODERATION_STATUSES
                );

        if (rows.isEmpty()) {
            return List.of();
        }

        Map<Long, DepartmentActivityStatsResponse> grouped = new LinkedHashMap<>();
        for (ActivityRepository.DepartmentStatusCountProjection row : rows) {
            Long departmentId = row.getDepartmentId();
            DepartmentActivityStatsResponse current = grouped.computeIfAbsent(
                    departmentId,
                    key -> DepartmentActivityStatsResponse.builder()
                            .departmentId(key)
                            .departmentName(key == null ? "Cấp Trường" : null)
                            .build()
            );

            long count = row.getTotal() == null ? 0L : row.getTotal();
            int status = row.getStatus() == null ? STATUS_PENDING : row.getStatus();
            if (status == STATUS_PENDING) {
                current.setPendingReview(count);
            } else if (status == STATUS_APPROVED) {
                current.setApprovedThisTerm(count);
            } else if (status == STATUS_REJECTED) {
                current.setRejected(count);
            }
        }

        List<Long> departmentIds = grouped.keySet().stream()
                .filter(Objects::nonNull)
                .toList();
        Map<Long, String> departmentNames = localDepartmentResolver.resolveDepartmentNames(departmentIds);

        grouped.values().forEach(item -> {
            if (item.getDepartmentId() == null) {
                item.setDepartmentName("Cấp Trường");
            } else {
                item.setDepartmentName(
                        departmentNames.getOrDefault(item.getDepartmentId(), "Đơn vị #" + item.getDepartmentId())
                );
            }
            item.setTotal(item.getPendingReview() + item.getApprovedThisTerm() + item.getRejected());
        });

        return grouped.values().stream()
                .sorted(Comparator.comparingLong(DepartmentActivityStatsResponse::getTotal).reversed()
                        .thenComparing(item -> item.getDepartmentName() == null ? "" : item.getDepartmentName()))
                .toList();
    }

    private org.springframework.data.jpa.domain.Specification<Activities> moderationScope() {
        return com.example.activityservice.feature.activities.specification.ActivitySpecification
                .hasCreatedByRoleType(ROLE_DEPARTMENT)
                .and(com.example.activityservice.feature.activities.specification.ActivitySpecification
                        .hasRequiresAdminApproval(true))
                .and(com.example.activityservice.feature.activities.specification.ActivitySpecification
                        .hasStatusIn(MODERATION_STATUSES));
    }

    private boolean isDepartmentSubmittedActivity(Activities activity) {
        return activity.getCreatedBy() != null
                && Integer.valueOf(ROLE_DEPARTMENT).equals(activity.getCreatedBy().getRoleType());
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

}
