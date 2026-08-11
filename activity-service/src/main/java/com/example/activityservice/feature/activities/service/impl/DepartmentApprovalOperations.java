package com.example.activityservice.feature.activities.service.impl;

import com.example.activityservice.feature.activities.dto.ActivityResponse;
import com.example.activityservice.feature.activities.dto.ActivityStatsResponse;
import com.example.activityservice.feature.activities.mapper.ActivityMapper;
import com.example.activityservice.feature.activities.model.Activities;
import com.example.activityservice.feature.activities.repository.ActivityRepository;
import com.example.activityservice.feature.activities.specification.ActivitySpecification;
import com.example.activityservice.feature.registration.repository.RegistrationRepository;
import com.example.activityservice.feature.users.model.Users;
import com.example.activityservice.feature.users.service.DepartmentRepresentativeClient;
import com.example.dto.PageDTO;
import com.example.exception.AppException;
import com.example.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class DepartmentApprovalOperations {

    private static final int ROLE_STUDENT = 1;
    private static final int ROLE_DEPARTMENT = 2;
    private static final int STATUS_PENDING = 0;
    private static final int STATUS_APPROVED = 1;
    private static final int STATUS_REJECTED = 2;

    private final ActivityRepository activityRepository;
    private final RegistrationRepository registrationRepository;
    private final ActivityMapper activityMapper;
    private final ActivityResponseAssembler responseAssembler;
    private final ActivityAccessSupport accessSupport;
    private final DepartmentRepresentativeClient representativeClient;

    @Transactional(readOnly = true)
    public PageDTO<ActivityResponse> getDepartmentApprovalActivities(
            String keyword,
            String status,
            Long classId,
            Pageable pageable) {
        Users reviewer = resolveDepartmentReviewer();
        List<Long> representativeIds = getRepresentativeStudentIds(reviewer.getDepartmentId(), classId);
        if (representativeIds.isEmpty()) {
            return activityMapper.toPageDTO(Page.empty(pageable), List.<ActivityResponse>of());
        }

        Specification<Activities> spec = baseApprovalSpec(reviewer.getDepartmentId(), keyword, representativeIds)
                .and(ActivitySpecification.hasStatusIn(resolveStatuses(status)));

        Page<Activities> pageActivities = activityRepository.findAll(spec, pageable);
        List<ActivityResponse> dtoList = pageActivities.getContent().stream()
                .map(this::toListResponse)
                .toList();
        responseAssembler.enrichDepartmentNames(dtoList);

        return activityMapper.toPageDTO(pageActivities, dtoList);
    }

    @Transactional(readOnly = true)
    public ActivityStatsResponse getDepartmentApprovalStats(String keyword, Long classId) {
        Users reviewer = resolveDepartmentReviewer();
        List<Long> representativeIds = getRepresentativeStudentIds(reviewer.getDepartmentId(), classId);
        if (representativeIds.isEmpty()) {
            return ActivityStatsResponse.builder()
                    .pendingReview(0)
                    .approvedThisTerm(0)
                    .rejected(0)
                    .build();
        }

        Specification<Activities> baseSpec = baseApprovalSpec(reviewer.getDepartmentId(), keyword, representativeIds);

        return ActivityStatsResponse.builder()
                .pendingReview(countByStatus(baseSpec, STATUS_PENDING))
                .approvedThisTerm(countByStatus(baseSpec, STATUS_APPROVED))
                .rejected(countByStatus(baseSpec, STATUS_REJECTED))
                .build();
    }

    private ActivityResponse toListResponse(Activities activity) {
        ActivityResponse response = responseAssembler.toResponseWithoutDepartment(activity);
        long count = registrationRepository.countByActivityIdAndStatusNot(activity.getId(), 2);
        response.setRegisteredCount((int) count);
        return response;
    }

    private long countByStatus(Specification<Activities> baseSpec, int status) {
        return activityRepository.count(baseSpec.and(ActivitySpecification.hasStatusIn(List.of(status))));
    }

    private Specification<Activities> baseApprovalSpec(
            Long departmentId,
            String keyword,
            List<Long> representativeIds) {
        Specification<Activities> spec = (root, query, cb) -> cb.conjunction();
        return spec.and(ActivitySpecification.hasDepartmentId(departmentId))
                .and(ActivitySpecification.hasCreatedByRoleType(ROLE_STUDENT))
                .and(ActivitySpecification.hasCreatedByIdIn(representativeIds))
                .and(ActivitySpecification.matchesApprovalKeyword(keyword));
    }

    private Users resolveDepartmentReviewer() {
        Users reviewer = accessSupport.getCurrentUserOrNull();
        if (reviewer == null
                || !Integer.valueOf(ROLE_DEPARTMENT).equals(reviewer.getRoleType())
                || reviewer.getDepartmentId() == null) {
            throw new AppException(ErrorCode.FORBIDDEN,
                    "Đơn vị chưa được gắn khoa/viện để duyệt hoạt động đại diện lớp.");
        }
        return reviewer;
    }

    private List<Long> getRepresentativeStudentIds(Long departmentId, Long classId) {
        return representativeClient.getRepresentatives(departmentId, classId, null).stream()
                .map(rep -> rep != null ? rep.getStudentId() : null)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private List<Integer> resolveStatuses(String status) {
        if (status == null || status.isBlank() || "PENDING".equalsIgnoreCase(status)) {
            return List.of(STATUS_PENDING);
        }
        if ("APPROVED".equalsIgnoreCase(status)) {
            return List.of(STATUS_APPROVED);
        }
        if ("REJECTED".equalsIgnoreCase(status)) {
            return List.of(STATUS_REJECTED);
        }
        if ("ALL".equalsIgnoreCase(status)) {
            return List.of(STATUS_PENDING, STATUS_APPROVED, STATUS_REJECTED);
        }
        throw new AppException(ErrorCode.INVALID_ACTION, "Trạng thái lọc hoạt động không hợp lệ.");
    }
}
