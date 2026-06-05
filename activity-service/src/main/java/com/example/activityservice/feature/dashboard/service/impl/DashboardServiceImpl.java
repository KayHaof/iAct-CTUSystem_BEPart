package com.example.activityservice.feature.dashboard.service.impl;

import com.example.activityservice.feature.activities.repository.ActivityRepository;
import com.example.activityservice.feature.activities.model.Activities;
import com.example.activityservice.feature.dashboard.dto.DashboardStatsResponse;
import com.example.activityservice.feature.dashboard.dto.RecentActivityDto;
import com.example.activityservice.feature.dashboard.service.DashboardService;
import com.example.activityservice.feature.registration.repository.RegistrationRepository;
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

    private final ActivityRepository activityRepository;
    private final RegistrationRepository registrationRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {
        long totalActivities = activityRepository.count();
        long pendingActivities = activityRepository.countByStatus(0);
        long activeActivities = activityRepository.countByStatus(1);

        // Use JPQL with JOIN FETCH to avoid LazyInitializationException
        List<Activities> recentActivitiesList = activityRepository.findRecentActivitiesWithOrganizer(
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "updatedAt"))).getContent();

        List<RecentActivityDto> recentActivities = recentActivitiesList.stream()
                .map(this::mapToRecentActivityDto)
                .collect(Collectors.toList());

        long totalStudents = registrationRepository.countDistinctStudentIds();

        return DashboardStatsResponse.builder()
                .totalActivities((int) totalActivities)
                .pendingActivities((int) pendingActivities)
                .activeActivities((int) activeActivities)
                .totalStudents((int) totalStudents)
                .totalDepartments(0)
                .totalMajors(0)
                .recentActivities(recentActivities)
                .build();
    }

    private RecentActivityDto mapToRecentActivityDto(Activities activity) {
        String departmentName = "N/A";
        if (activity.getOrganizer() != null && activity.getOrganizer().getName() != null) {
            departmentName = activity.getOrganizer().getName();
        }

        String startDateStr = null;
        if (activity.getStartDate() != null) {
            startDateStr = activity.getStartDate().toString();
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
