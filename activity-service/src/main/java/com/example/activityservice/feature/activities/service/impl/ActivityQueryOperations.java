package com.example.activityservice.feature.activities.service.impl;

import com.example.activityservice.feature.activities.dto.ActivityResponse;
import com.example.activityservice.feature.activities.dto.ActivityTimeLocationResponse;
import com.example.activityservice.feature.activities.mapper.ActivityMapper;
import com.example.activityservice.feature.activities.model.Activities;
import com.example.activityservice.feature.activities.repository.ActivityRepository;
import com.example.activityservice.feature.activities.specification.ActivitySpecification;
import com.example.activityservice.feature.organizers.model.Organizers;
import com.example.activityservice.feature.organizers.repository.OrganizerRepository;
import com.example.activityservice.feature.registration.repository.RegistrationRepository;
import com.example.activityservice.feature.users.model.Users;
import com.example.activityservice.feature.users.repository.UserRepository;
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
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ActivityQueryOperations {

    private final ActivityRepository activityRepository;
    private final OrganizerRepository organizerRepository;
    private final UserRepository userRepository;
    private final RegistrationRepository registrationRepository;
    private final ActivityMapper activityMapper;
    private final ActivityResponseAssembler responseAssembler;
    private final ActivityAccessSupport accessSupport;

    @Transactional(readOnly = true)
    public ActivityResponse getActivityById(Long id) {
        Activities activity = activityRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Khong tim thay hoat dong"));

        Users currentUser = accessSupport.getCurrentUserOrNull();
        if (accessSupport.isCurrentStudent() && !accessSupport.isVisibleToStudent(activity, currentUser)) {
            throw new AppException(ErrorCode.FORBIDDEN, "Ban khong co quyen xem hoat dong nay.");
        }

        ActivityResponse response = responseAssembler.toResponse(activity);
        response.setBenefits(responseAssembler.getActivityBenefits(activity.getId()));
        long count = registrationRepository.countByActivityIdAndStatusNot(id, 2);
        response.setRegisteredCount((int) count);
        return response;
    }

    public ActivityTimeLocationResponse getActivityTimesAndLocation(Long id) {
        Activities activity = activityRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED,
                        "Khong tim thay hoat dong voi ID: " + id));
        return activityMapper.toTimeResponse(activity);
    }

    @Transactional(readOnly = true)
    public PageDTO<ActivityResponse> getAllActivities(String keyword, String level, String status, Long departmentId,
            Pageable pageable) {
        Long userDeptId = null;
        boolean isAdmin = false;
        boolean isDepartment = false;
        boolean isStudent = true;
        Users currentUser = null;

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(auth -> Objects.equals(auth.getAuthority(), "ROLE_ADMIN"));
            isDepartment = authentication.getAuthorities().stream()
                    .anyMatch(auth -> Objects.equals(auth.getAuthority(), "ROLE_DEPARTMENT"));
            isStudent = !isAdmin && !isDepartment;

            String username = authentication.getName();
            Optional<Users> userOpt = userRepository.findByUsername(username);

            if (userOpt.isPresent()) {
                currentUser = userOpt.get();
                if (isAdmin || isDepartment) {
                    Optional<Organizers> orgOpt = organizerRepository.findById(currentUser.getId());
                    if (orgOpt.isPresent() && orgOpt.get().getDepartmentId() != null) {
                        userDeptId = orgOpt.get().getDepartmentId();
                    }
                } else {
                    userDeptId = currentUser.getDepartmentId();
                }
            }
        }

        Specification<Activities> spec = (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();

        if (isStudent) {
            spec = spec.and(ActivitySpecification.isApproved());
            spec = spec.and(ActivitySpecification.visibleToStudentDepartment(userDeptId));
        } else if (isDepartment && currentUser != null) {
            spec = spec.and(ActivitySpecification.isOwnedByOrOrganizedBy(currentUser.getId()));
        } else if (isAdmin) {
            spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.notEqual(root.get("status"), 3));
        }

        if (departmentId != null) {
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
}
