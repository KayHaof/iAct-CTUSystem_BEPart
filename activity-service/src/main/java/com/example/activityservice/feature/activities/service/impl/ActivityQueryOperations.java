package com.example.activityservice.feature.activities.service.impl;

import com.example.activityservice.feature.activities.dto.ActivityResponse;
import com.example.activityservice.feature.activities.dto.ActivityTimeLocationResponse;
import com.example.activityservice.feature.activities.mapper.ActivityMapper;
import com.example.activityservice.feature.activities.model.Activities;
import com.example.activityservice.feature.activities.repository.ActivityRepository;
import com.example.activityservice.feature.activities.specification.ActivitySpecification;
// import com.example.activityservice.feature.organizers.repository.OrganizerRepository;
import com.example.activityservice.feature.registration.repository.RegistrationRepository;
import com.example.activityservice.feature.users.model.Users;
// import com.example.activityservice.feature.users.repository.UserRepository;
import com.example.dto.PageDTO;
import com.example.exception.AppException;
import com.example.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ActivityQueryOperations {
    private static final int STATUS_APPROVED = 1;

    private final ActivityRepository activityRepository;
    // private final OrganizerRepository organizerRepository;
    // private final UserRepository userRepository;
    private final RegistrationRepository registrationRepository;
    private final ActivityMapper activityMapper;
    private final ActivityResponseAssembler responseAssembler;
    private final ActivityAccessSupport accessSupport;

    @Transactional(readOnly = true)
    public ActivityResponse getActivityById(Long id) {
        Activities activity = activityRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Không tìm thấy hoạt động"));

        ensureCurrentAdminCanRead(activity);
        accessSupport.ensureCurrentDepartmentCanRead(activity);
        ensureCurrentStudentCanRead(activity);

        return buildDetailResponse(activity);
    }

    @Transactional(readOnly = true)
    public ActivityResponse getMyCreatedActivity(Long id) {
        Activities activity = activityRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Không tìm thấy hoạt động"));

        Users currentUser = accessSupport.getCurrentUserOrNull();
        if (!accessSupport.isCreatedBy(activity, currentUser)) {
            throw new AppException(ErrorCode.FORBIDDEN, "Bạn không có quyền xem đề xuất hoạt động này.");
        }

        return buildDetailResponse(activity);
    }

    private ActivityResponse buildDetailResponse(Activities activity) {
        ActivityResponse response = responseAssembler.toResponse(activity);
        response.setBenefits(responseAssembler.getActivityBenefits(activity.getId()));
        long count = registrationRepository.countByActivityIdAndStatusNot(activity.getId(), 2);
        response.setRegisteredCount((int) count);
        return response;
    }

    @Transactional(readOnly = true)
    public ActivityTimeLocationResponse getActivityTimesAndLocation(Long id) {
        Activities activity = activityRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED,
                        "Không tìm thấy hoạt động với ID: " + id));
        ensureCurrentAdminCanRead(activity);
        accessSupport.ensureCurrentDepartmentCanRead(activity);
        ensureCurrentStudentCanRead(activity);
        return activityMapper.toTimeResponse(activity);
    }

    private void ensureCurrentStudentCanRead(Activities activity) {
        Users currentUser = accessSupport.getCurrentUserOrNull();
        if (!accessSupport.isCurrentStudent() || accessSupport.isCreatedBy(activity, currentUser)) {
            return;
        }

        boolean isApproved = Integer.valueOf(STATUS_APPROVED).equals(activity.getStatus());
        if (!isApproved || !accessSupport.isVisibleToStudent(activity, currentUser)) {
            throw new AppException(ErrorCode.FORBIDDEN, "Bạn không có quyền xem hoạt động này.");
        }
    }

    private void ensureCurrentAdminCanRead(Activities activity) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return;
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> Objects.equals(auth.getAuthority(), "ROLE_ADMIN"));
        if (!isAdmin) {
            return;
        }

        if (activity.getCreatedBy() == null || !Integer.valueOf(2).equals(activity.getCreatedBy().getRoleType())) {
            throw new AppException(ErrorCode.FORBIDDEN, "Admin chỉ xem hoạt động do Khoa/Đơn vị gửi lên.");
        }
    }

    @Transactional(readOnly = true)
    public PageDTO<ActivityResponse> getAllActivities(String keyword, String level, String status, Long departmentId,
            boolean adminApprovalOnly,
            Pageable pageable) {
        final int ROLE_DEPARTMENT = 2;
        Long userDeptId = null;
        boolean isAdmin = false;
        boolean isDepartment = false;
        boolean isStudent = true;
        Users currentUser = null;

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            isAdmin = accessSupport.isCurrentAdmin();
            isDepartment = accessSupport.isCurrentDepartment();
            isStudent = !isAdmin && !isDepartment;

            currentUser = accessSupport.getCurrentUserOrNull();
            if (currentUser != null) {
                userDeptId = currentUser.getDepartmentId();
            }
        }

        Specification<Activities> spec = (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();

        if (isStudent) {
            spec = spec.and(ActivitySpecification.isApproved());
            spec = spec.and(ActivitySpecification.visibleToStudentDepartment(userDeptId));
        } else if (isDepartment) {
            userDeptId = accessSupport.requireCurrentDepartmentId();
            spec = spec.and(ActivitySpecification.hasDepartmentId(userDeptId));
        } else if (isAdmin) {
            spec = spec.and(ActivitySpecification.hasCreatedByRoleType(ROLE_DEPARTMENT))
                    .and(ActivitySpecification.hasStatusIn(List.of(0, 1, 2)));
            if (adminApprovalOnly) {
                spec = spec.and(ActivitySpecification.hasRequiresAdminApproval(true));
            }
        }

        if (departmentId != null && isAdmin) {
            spec = spec.and(ActivitySpecification.hasDepartmentId(departmentId));
        }

        boolean isOrganizer = isAdmin || isDepartment;
        spec = spec.and(ActivitySpecification.containsKeyword(keyword))
                .and(ActivitySpecification.hasLevel(level, userDeptId))
                .and(ActivitySpecification.hasStatus(status, keyword, isOrganizer));

        Page<Activities> pageActivities = activityRepository.findAll(spec, pageable);

        List<ActivityResponse> dtoList = pageActivities.getContent().stream()
                .map(activity -> {
                    ActivityResponse response = responseAssembler.toResponseWithoutDepartment(activity);
                    long count = registrationRepository.countByActivityIdAndStatusNot(activity.getId(), 2);
                    response.setRegisteredCount((int) count);
                    return response;
                })
                .collect(Collectors.toList());
        responseAssembler.enrichDepartmentNames(dtoList);

        return activityMapper.toPageDTO(pageActivities, dtoList);
    }

    @Transactional(readOnly = true)
    public PageDTO<ActivityResponse> getMyCreatedActivities(Pageable pageable) {
        Users currentUser = accessSupport.getCurrentUserOrNull();
        if (currentUser == null) {
            throw new AppException(ErrorCode.FORBIDDEN, "Vui lòng đăng nhập để xem hoạt động đã gửi.");
        }

        Page<Activities> pageActivities = activityRepository.findByCreatedById(currentUser.getId(), pageable);
        List<ActivityResponse> dtoList = pageActivities.getContent().stream()
                .map(activity -> {
                    ActivityResponse response = responseAssembler.toResponse(activity);
                    long count = registrationRepository.countByActivityIdAndStatusNot(activity.getId(), 2);
                    response.setRegisteredCount((int) count);
                    response.setBenefits(responseAssembler.getActivityBenefits(activity.getId()));
                    return response;
                })
                .collect(Collectors.toList());
        responseAssembler.enrichDepartmentNames(dtoList);

        return activityMapper.toPageDTO(pageActivities, dtoList);
    }
}
