package com.example.activityservice.feature.activities.service.impl;

import com.example.activityservice.feature.activities.dto.ActivityResponse;
import com.example.activityservice.feature.activities.mapper.ActivityMapper;
import com.example.activityservice.feature.activities.model.Activities;
import com.example.activityservice.feature.benefits.dto.BenefitResponse;
import com.example.activityservice.feature.benefits.mapper.BenefitMapper;
import com.example.activityservice.feature.benefits.repository.BenefitRepository;
import com.example.activityservice.feature.locations.service.ActivityLocationBookingService;
import com.example.activityservice.feature.users.service.LocalDepartmentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class ActivityResponseAssembler {

    private final ActivityMapper activityMapper;
    private final BenefitMapper benefitMapper;
    private final BenefitRepository benefitRepository;
    private final LocalDepartmentResolver localDepartmentResolver;
    private final ActivityLocationBookingService locationBookingService;

    public ActivityResponse toResponse(Activities activity) {
        ActivityResponse response = activityMapper.toResponse(activity);
        enrichDisplayFields(response);
        return response;
    }

    public ActivityResponse toResponseWithoutDepartment(Activities activity) {
        ActivityResponse response = activityMapper.toResponse(activity);
        enrichLocalDisplayFields(response);
        return response;
    }

    public void enrichDepartmentNames(List<ActivityResponse> responses) {
        List<Long> departmentIds = responses.stream()
                .filter(Objects::nonNull)
                .flatMap(response -> java.util.stream.Stream.of(
                        response.getDepartmentId(),
                        response.getOrganizer() == null ? null : response.getOrganizer().getDepartmentId()))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, String> departmentNames = localDepartmentResolver.resolveDepartmentNames(departmentIds);
        responses.forEach(response -> {
            if (response.getDepartmentName() == null && response.getDepartmentId() != null) {
                response.setDepartmentName(departmentNames.get(response.getDepartmentId()));
            }
            enrichOrganizerDepartmentName(response, departmentNames);
        });
    }

    public List<BenefitResponse> getActivityBenefits(Long activityId) {
        return benefitRepository.findByActivityId(activityId).stream()
                .map(benefitMapper::toResponse)
                .toList();
    }

    public String getBenefitCategoryNames(Long activityId) {
        List<String> categoryNames = benefitRepository.findByActivityId(activityId).stream()
                .map(benefit -> benefit.getCategory())
                .filter(Objects::nonNull)
                .map(category -> category.getName())
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        return categoryNames.isEmpty() ? null : String.join(", ", categoryNames);
    }

    private void enrichDisplayFields(ActivityResponse response) {
        enrichLocalDisplayFields(response);
        enrichDepartmentName(response);
        enrichOrganizerDepartmentName(response);
    }

    private void enrichLocalDisplayFields(ActivityResponse response) {
        if (response == null) {
            return;
        }
        response.setSemesterDisplayName(buildSemesterDisplayName(response));
        response.setStatusLabel(toStatusLabel(response.getStatus()));
        if (response.getId() != null) {
            response.setLocationBookings(locationBookingService.getBookingsByActivityId(response.getId()));
        }
    }

    private void enrichDepartmentName(ActivityResponse response) {
        if (response.getDepartmentName() == null && response.getDepartmentId() != null) {
            localDepartmentResolver.resolveDepartmentName(response.getDepartmentId())
                    .ifPresent(response::setDepartmentName);
        }
    }

    private void enrichOrganizerDepartmentName(ActivityResponse response) {
        if (response.getOrganizer() == null
                || response.getOrganizer().getDepartmentName() != null
                || response.getOrganizer().getDepartmentId() == null) {
            return;
        }
        localDepartmentResolver.resolveDepartmentName(response.getOrganizer().getDepartmentId())
                .ifPresent(response.getOrganizer()::setDepartmentName);
    }

    private void enrichOrganizerDepartmentName(ActivityResponse response, Map<Long, String> departmentNames) {
        if (response.getOrganizer() == null
                || response.getOrganizer().getDepartmentName() != null
                || response.getOrganizer().getDepartmentId() == null) {
            return;
        }
        response.getOrganizer().setDepartmentName(departmentNames.get(response.getOrganizer().getDepartmentId()));
    }

    private String buildSemesterDisplayName(ActivityResponse response) {
        if (response.getSemesterName() == null && response.getAcademicYear() == null) {
            return null;
        }
        if (response.getSemesterName() == null) {
            return "Năm học " + response.getAcademicYear();
        }
        if (response.getAcademicYear() == null) {
            return response.getSemesterName();
        }
        return response.getSemesterName() + ", năm học " + response.getAcademicYear();
    }

    private String toStatusLabel(Integer status) {
        if (status == null) {
            return "Đang cập nhật";
        }
        return switch (status) {
            case 0 -> "Chờ duyệt";
            case 1 -> "Đã duyệt";
            case 2 -> "Từ chối";
            case 3 -> "Bản nháp";
            case 4 -> "Đã hủy";
            default -> "Đang cập nhật";
        };
    }
}
