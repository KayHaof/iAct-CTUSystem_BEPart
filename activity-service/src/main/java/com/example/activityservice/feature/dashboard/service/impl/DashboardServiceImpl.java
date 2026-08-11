package com.example.activityservice.feature.dashboard.service.impl;

import com.example.util.UtcDateTime;
import com.example.activityservice.feature.activities.repository.ActivityRepository;
import com.example.activityservice.feature.activities.model.Activities;
import com.example.activityservice.feature.activities.service.impl.ActivityAccessSupport;
import com.example.activityservice.feature.dashboard.dto.DashboardStatsResponse;
import com.example.activityservice.feature.dashboard.dto.RecentActivityDto;
import com.example.activityservice.feature.dashboard.service.DashboardService;
import com.example.activityservice.feature.dashboard.service.StatsCacheService;
import com.example.activityservice.feature.registration.repository.RegistrationRepository;
import com.example.activityservice.feature.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private static final int ROLE_STUDENT = 1;

    private final ActivityRepository activityRepository;
    private final RegistrationRepository registrationRepository;
    private final UserRepository userRepository;
    private final StatsCacheService statsCacheService;
    private final ActivityAccessSupport accessSupport;

    @Override
    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {
        if (accessSupport.isCurrentDepartment()) {
            return getDepartmentDashboardStats(accessSupport.requireCurrentDepartmentId());
        }
        if (!accessSupport.isCurrentAdmin()) {
            throw new com.example.exception.AppException(
                    com.example.exception.ErrorCode.FORBIDDEN,
                    "Bạn không có quyền xem dashboard quản trị.");
        }
        return getSystemDashboardStats();
    }

    private DashboardStatsResponse getSystemDashboardStats() {
        long totalActivities = activityRepository.count();
        long pendingActivities = activityRepository.countByStatus(0);
        long activeActivities = activityRepository.countByStatus(1);

        List<Activities> recentActivitiesList = activityRepository.findRecentActivitiesWithOrganizer(
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "updatedAt"))).getContent();

        List<RecentActivityDto> recentActivities = recentActivitiesList.stream()
                .map(this::mapToRecentActivityDto)
                .collect(Collectors.toList());

        long totalStudents = userRepository.countByRoleType(ROLE_STUDENT);

        return DashboardStatsResponse.builder()
                .totalActivities((int) totalActivities)
                .pendingActivities((int) pendingActivities)
                .activeActivities((int) activeActivities)
                .totalStudents((int) totalStudents)
                .totalDepartments((int) statsCacheService.getCachedDepartmentCount())
                .totalMajors((int) statsCacheService.getCachedMajorCount())
                .recentActivities(recentActivities)
                .build();
    }

    private DashboardStatsResponse getDepartmentDashboardStats(Long departmentId) {
        long totalActivities = activityRepository.countByDepartmentId(departmentId);
        long pendingActivities = activityRepository.countByDepartmentIdAndStatus(departmentId, 0);
        long activeActivities = activityRepository.countByDepartmentIdAndStatus(departmentId, 1);

        List<Activities> recentActivitiesList = activityRepository.findRecentActivitiesWithOrganizerByDepartmentId(
                departmentId,
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "updatedAt"))).getContent();

        List<RecentActivityDto> recentActivities = recentActivitiesList.stream()
                .map(this::mapToRecentActivityDto)
                .collect(Collectors.toList());

        long totalStudents = registrationRepository.countDistinctStudentIdsByActivityDepartmentId(departmentId);

        return DashboardStatsResponse.builder()
                .totalActivities((int) totalActivities)
                .pendingActivities((int) pendingActivities)
                .activeActivities((int) activeActivities)
                .totalStudents((int) totalStudents)
                .totalDepartments(1)
                .totalMajors(0)
                .recentActivities(recentActivities)
                .build();
    }

    private RecentActivityDto mapToRecentActivityDto(Activities activity) {
        String departmentName = "N/A";
        if (activity.getOrganizer() != null && activity.getOrganizer().getFullName() != null) {
            departmentName = activity.getOrganizer().getFullName();
        }

        String startDateStr = null;
        if (activity.getStartDate() != null) {
            startDateStr = UtcDateTime.format(activity.getStartDate());
        }

        return RecentActivityDto.builder()
                .id(activity.getId())
                .title(activity.getTitle())
                .departmentName(departmentName)
                .startDate(startDateStr)
                .status(activity.getStatus())
                .registeredCount((long) registrationRepository.countByActivityIdAndStatusNot(activity.getId(), 2))
                .maxParticipants(activity.getMaxParticipants())
                .thumbnail(activity.getCoverImage())
                .build();
    }
}
