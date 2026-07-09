package com.example.activityservice.feature.activities.service.impl;

import com.example.activityservice.feature.activities.dto.ActivityResponse;
import com.example.activityservice.feature.activities.dto.RecommendationResponse;
import com.example.activityservice.feature.activities.dto.RecommendedActivity;
import com.example.activityservice.feature.activities.model.Activities;
import com.example.activityservice.feature.activities.repository.ActivityRepository;
import com.example.activityservice.feature.activities.service.ActivityCacheService;
import com.example.activityservice.feature.activities.specification.ActivitySpecification;
import com.example.activityservice.feature.registration.repository.RegistrationRepository;
import com.example.activityservice.feature.semesters.model.Semesters;
import com.example.activityservice.feature.semesters.repository.SemesterRepository;
import com.example.activityservice.feature.users.model.Users;
import com.example.activityservice.feature.users.repository.UserRepository;
import com.example.activityservice.feature.users.service.LocalDepartmentResolver;
import com.example.dto.PageDTO;
import com.example.exception.AppException;
import com.example.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentActivityOperations {

    private final ActivityRepository activityRepository;
    private final SemesterRepository semesterRepository;
    private final RegistrationRepository registrationRepository;
    private final UserRepository userRepository;
    private final LocalDepartmentResolver localDepartmentResolver;
    private final ActivityResponseAssembler responseAssembler;
    private final ActivityAccessSupport accessSupport;
    private final ActivityCacheService activityCacheService;

    @Transactional(readOnly = true)
    public PageDTO<ActivityResponse> searchActivities(
            String keyword, Long departmentId, String startDate, String endDate,
            List<Long> categoryIds, String category, String status, Pageable pageable) {

        Long currentStudentDepartmentId = accessSupport.currentStudentDepartmentId();
        var cached = activityCacheService.getSearch(keyword, departmentId, startDate, endDate, categoryIds, category,
                status, currentStudentDepartmentId, pageable);
        if (cached.isPresent()) {
            return cached.get();
        }

        Specification<Activities> spec = (root, query, cb) -> cb.conjunction();
        spec = spec.and(ActivitySpecification.isApproved());
        spec = spec.and(ActivitySpecification.visibleToStudentDepartment(currentStudentDepartmentId));

        if (keyword != null && !keyword.isBlank()) {
            spec = spec.and(ActivitySpecification.containsKeyword(keyword));
        }

        if (departmentId != null) {
            spec = spec.and(ActivitySpecification.hasDepartmentId(departmentId));
        }

        if (categoryIds != null && !categoryIds.isEmpty()) {
            spec = spec.and(ActivitySpecification.hasBenefitCategories(categoryIds));
        }

        if (startDate != null && !startDate.isBlank()) {
            LocalDate start = LocalDate.parse(startDate);
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("startDate"), start.atStartOfDay()));
        }

        if (endDate != null && !endDate.isBlank()) {
            LocalDate end = LocalDate.parse(endDate);
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("endDate"), end.atTime(23, 59, 59)));
        }

        Page<Activities> page = activityRepository.findAll(spec, pageable);
        List<ActivityResponse> dtoList = page.getContent().stream()
                .map(activity -> {
                    ActivityResponse response = responseAssembler.toResponseWithoutDepartment(activity);
                    response.setRegisteredCount(
                            (int) registrationRepository.countByActivityIdAndStatusNot(activity.getId(), 2));
                    return response;
                })
                .collect(Collectors.toList());
        responseAssembler.enrichDepartmentNames(dtoList);

        PageDTO<ActivityResponse> response = newPageDTO(page, dtoList);
        activityCacheService.putSearch(keyword, departmentId, startDate, endDate, categoryIds, category, status,
                currentStudentDepartmentId, pageable, response);
        return response;
    }

    @Transactional(readOnly = true)
    public RecommendationResponse getRecommendations(Long studentId, int limit, Jwt jwt) {
        if (studentId == null && jwt != null) {
            String username = jwt.getClaimAsString("preferred_username");
            Users student = userRepository.findByUsername(username).orElse(null);
            if (student != null) {
                studentId = student.getId();
            }
        }

        if (studentId == null) {
            return RecommendationResponse.builder()
                    .activities(List.of())
                    .reasons(List.of())
                    .totalFound(0)
                    .build();
        }

        Semesters semester = semesterRepository.findSemesterByDate(LocalDate.now()).orElse(null);
        if (semester == null) {
            return RecommendationResponse.builder()
                    .activities(List.of())
                    .reasons(List.of("Khong co hoc ky hien tai"))
                    .totalFound(0)
                    .build();
        }

        var cached = activityCacheService.getRecommendations(studentId, semester.getId(), limit);
        if (cached.isPresent()) {
            return cached.get();
        }

        Users student = userRepository.findById(studentId).orElse(null);
        List<Activities> approvedActivities = activityRepository.findApprovedActivitiesForStudent(semester.getId());
        Map<Long, String> departmentNames = localDepartmentResolver.resolveDepartmentNames(approvedActivities.stream()
                .map(activity -> activity.getDepartmentId())
                .toList());

        List<RecommendedActivity> recommended = approvedActivities.stream()
                .filter(activity -> accessSupport.isVisibleToStudent(activity, student))
                .limit(limit)
                .map(activity -> RecommendedActivity.builder()
                        .id(activity.getId())
                        .title(activity.getTitle())
                        .description(activity.getDescription())
                        .location(activity.getLocation())
                        .startDate(activity.getStartDate() != null ? activity.getStartDate().toString() : null)
                        .endDate(activity.getEndDate() != null ? activity.getEndDate().toString() : null)
                        .maxParticipants(activity.getMaxParticipants())
                        .registeredCount(
                                (int) registrationRepository.countByActivityIdAndStatusNot(activity.getId(), 2))
                        .matchPercentage(85.0)
                        .matchedReasons(List.of("Hoat dong phu hop voi yeu cau diem ren luyen"))
                        .categoryName(responseAssembler.getBenefitCategoryNames(activity.getId()))
                        .departmentName(departmentNames.get(activity.getDepartmentId()))
                        .build())
                .collect(Collectors.toList());

        RecommendationResponse response = RecommendationResponse.builder()
                .activities(recommended)
                .reasons(List.of("Cac hoat dong duoc goi y dua tren diem ren luyen con thieu"))
                .totalFound(recommended.size())
                .build();
        activityCacheService.putRecommendations(studentId, semester.getId(), limit, response);
        return response;
    }

    @Transactional(readOnly = true)
    public PageDTO<ActivityResponse> getActivitiesForRegistration(Long semesterId, Pageable pageable) {
        final Long resolvedSemesterId;
        if (semesterId != null) {
            resolvedSemesterId = semesterId;
        } else {
            Semesters semester = semesterRepository.findSemesterByDate(LocalDate.now())
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Khong co hoc ky"));
            resolvedSemesterId = semester.getId();
        }

        Long currentStudentDepartmentId = accessSupport.currentStudentDepartmentId();
        var cached = activityCacheService.getForRegistration(resolvedSemesterId, currentStudentDepartmentId, pageable);
        if (cached.isPresent()) {
            return cached.get();
        }

        LocalDateTime now = LocalDateTime.now();

        Specification<Activities> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), 1));
            predicates.add(cb.lessThanOrEqualTo(root.get("registrationStart"), now));
            predicates.add(cb.greaterThanOrEqualTo(root.get("registrationEnd"), now));
            predicates.add(cb.equal(root.get("semester").get("id"), resolvedSemesterId));
            predicates.add(ActivitySpecification.visibleToStudentDepartment(currentStudentDepartmentId)
                    .toPredicate(root, query, cb));
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<Activities> page = activityRepository.findAll(spec, pageable);
        List<ActivityResponse> dtoList = page.getContent().stream()
                .map(activity -> {
                    ActivityResponse response = responseAssembler.toResponseWithoutDepartment(activity);
                    response.setRegisteredCount(
                            (int) registrationRepository.countByActivityIdAndStatusNot(activity.getId(), 2));
                    return response;
                })
                .collect(Collectors.toList());
        responseAssembler.enrichDepartmentNames(dtoList);

        PageDTO<ActivityResponse> response = newPageDTO(page, dtoList);
        activityCacheService.putForRegistration(resolvedSemesterId, currentStudentDepartmentId, pageable, response);
        return response;
    }

    private PageDTO<ActivityResponse> newPageDTO(Page<Activities> page, List<ActivityResponse> dtoList) {
        PageDTO<ActivityResponse> result = new PageDTO<>();
        result.setPageNumber(page.getNumber() + 1);
        result.setPageSize(page.getSize());
        result.setTotalPage(page.getTotalPages());
        result.setTotalRows(page.getTotalElements());
        result.setLast(page.isLast());
        result.setData(dtoList);
        return result;
    }
}
