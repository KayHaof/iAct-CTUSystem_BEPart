package com.example.userservice.feature.departments.service.impl;

import com.example.dto.PageDTO;
import com.example.exception.AppException;
import com.example.exception.ErrorCode;
import com.example.userservice.feature.departments.dto.DepartmentRequest;
import com.example.userservice.feature.departments.dto.DepartmentResponse;
import com.example.userservice.feature.departments.mapper.DepartmentMapper;
import com.example.userservice.feature.departments.model.Departments;
import com.example.userservice.feature.departments.repository.DepartmentRepository;
import com.example.userservice.feature.departments.service.DepartmentService;
import com.example.userservice.feature.major.repository.MajorRepository;
import com.example.userservice.feature.user_profile.model.DepartmentProfile;
import com.example.userservice.feature.user_profile.repository.DepartmentProfileRepository;
import com.example.userservice.feature.users.model.Users;
import com.example.userservice.feature.users.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private static final int ROLE_DEPARTMENT = 2;
    private static final String LOCAL_DEPARTMENT_KEYCLOAK_PREFIX = "local-department-";

    private final DepartmentRepository departmentRepository;
    private final MajorRepository majorRepository;
    private final DepartmentProfileRepository departmentProfileRepository;
    private final UserRepository userRepository;
    private final DepartmentMapper departmentMapper;

    @Override
    @Transactional
    public DepartmentResponse createDepartment(DepartmentRequest request) {
        validateCodeUnique(request.getCode(), null);

        Departments department = new Departments();
        applyRequest(department, request);
        Departments savedDepartment = departmentRepository.save(department);
        ensureDepartmentProfile(savedDepartment, request);

        return toResponse(savedDepartment);
    }

    @Override
    @Transactional
    public DepartmentResponse updateDepartment(Long id, DepartmentRequest request) {
        Departments department = findDepartmentOrThrow(id);
        validateCodeUnique(request.getCode(), id);
        applyRequest(department, request);

        Departments savedDepartment = departmentRepository.save(department);
        ensureDepartmentProfile(savedDepartment, request);
        syncDepartmentUserStatus(savedDepartment.getId(), savedDepartment.getIsActive());

        return toResponse(savedDepartment);
    }

    @Override
    @Transactional
    public void deleteDepartment(Long id) {
        Departments department = findDepartmentOrThrow(id);
        if (majorRepository.existsByDepartmentId(id)) {
            department.setIsActive(false);
            departmentRepository.save(department);
            syncDepartmentUserStatus(id, false);
            return;
        }

        deleteInternalDepartmentProfiles(id);
        departmentRepository.delete(department);
    }

    @Override
    @Transactional
    public DepartmentResponse activateDepartment(Long id) {
        Departments department = findDepartmentOrThrow(id);
        department.setIsActive(true);
        Departments savedDepartment = departmentRepository.save(department);
        ensureDepartmentProfile(savedDepartment, null);
        syncDepartmentUserStatus(id, true);
        return toResponse(savedDepartment);
    }

    @Override
    @Transactional
    public DepartmentResponse deactivateDepartment(Long id) {
        Departments department = findDepartmentOrThrow(id);
        department.setIsActive(false);
        Departments savedDepartment = departmentRepository.save(department);
        syncDepartmentUserStatus(id, false);
        return toResponse(savedDepartment);
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentResponse getDepartmentById(Long id) {
        return toResponse(findDepartmentOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageDTO<DepartmentResponse> getDepartments(int page, int size, String keyword, Boolean active) {
        Pageable pageable = PageRequest.of(
                page > 0 ? page - 1 : 0,
                size,
                Sort.by(Sort.Direction.DESC, "id")
        );

        Specification<Departments> spec = buildSpecification(keyword, active);
        Page<Departments> departmentPage = departmentRepository.findAll(spec, pageable);

        List<DepartmentResponse> data = departmentPage.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return new PageDTO<>(departmentPage, data);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentResponse> getDepartmentOptions(Boolean active) {
        Sort sort = Sort.by(Sort.Direction.ASC, "name");
        List<Departments> departments = active == null
                ? departmentRepository.findAll(sort)
                : departmentRepository.findByIsActive(active, sort);

        return departments.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentResponse> getAllDepartments() {
        return getDepartmentOptions(null);
    }

    private Specification<Departments> buildSpecification(String keyword, Boolean active) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (active != null) {
                predicates.add(cb.equal(root.get("isActive"), active));
            }

            if (keyword != null && !keyword.trim().isEmpty()) {
                String likeKeyword = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), likeKeyword),
                        cb.like(cb.lower(root.get("code")), likeKeyword),
                        cb.like(cb.lower(root.get("description")), likeKeyword)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private DepartmentResponse toResponse(Departments department) {
        DepartmentResponse response = departmentMapper.toResponse(department);
        departmentProfileRepository.findFirstByDepartmentId(department.getId()).ifPresent(profile -> {
            response.setProfileUserId(profile.getUserId());
            response.setPhone(profile.getPhone());
            response.setAddress(profile.getAddress());
            response.setAvatarUrl(profile.getAvatarUrl());
        });
        return response;
    }

    private void ensureDepartmentProfile(Departments department, DepartmentRequest request) {
        DepartmentProfile profile = departmentProfileRepository.findFirstByDepartmentId(department.getId())
                .orElseGet(() -> DepartmentProfile.builder()
                        .userId(createInternalDepartmentUser(department).getId())
                        .department(department)
                        .build());

        profile.setDepartment(department);
        profile.setFullName(department.getName());
        if (request != null) {
            profile.setPhone(normalizeNullableText(request.getPhone()));
            profile.setAddress(normalizeNullableText(request.getAddress()));
            profile.setAvatarUrl(normalizeNullableText(request.getAvatarUrl()));
        }
        departmentProfileRepository.save(profile);
    }

    private Users createInternalDepartmentUser(Departments department) {
        String identity = getDepartmentIdentity(department);
        String username = buildUniqueUsername("dept_" + identity.toLowerCase(Locale.ROOT), department.getId());
        String email = buildUniqueEmail(username, department.getId());

        Users user = Users.builder()
                .keycloakId(LOCAL_DEPARTMENT_KEYCLOAK_PREFIX + department.getId())
                .username(username)
                .email(email)
                .roleType(ROLE_DEPARTMENT)
                .status(Boolean.FALSE.equals(department.getIsActive()) ? 0 : 1)
                .build();
        return userRepository.save(user);
    }

    private void syncDepartmentUserStatus(Long departmentId, Boolean isActive) {
        departmentProfileRepository.findFirstByDepartmentId(departmentId)
                .flatMap(profile -> userRepository.findById(profile.getUserId()))
                .ifPresent(user -> {
                    user.setStatus(Boolean.FALSE.equals(isActive) ? 0 : 1);
                    userRepository.save(user);
                });
    }

    private void deleteInternalDepartmentProfiles(Long departmentId) {
        List<DepartmentProfile> profiles = departmentProfileRepository.findByDepartmentId(departmentId);
        for (DepartmentProfile profile : profiles) {
            userRepository.findById(profile.getUserId()).ifPresent(user -> {
                if (isInternalDepartmentUser(user)) {
                    userRepository.delete(user);
                } else {
                    profile.setDepartment(null);
                    departmentProfileRepository.save(profile);
                }
            });
        }
    }

    private void applyRequest(Departments department, DepartmentRequest request) {
        department.setName(request.getName().trim());
        department.setCode(normalizeCode(request.getCode()));
        department.setDescription(normalizeNullableText(request.getDescription()));
        department.setIsActive(request.getIsActive() == null ? Boolean.TRUE : request.getIsActive());
    }

    private void validateCodeUnique(String rawCode, Long currentId) {
        String code = normalizeCode(rawCode);
        if (code == null) {
            return;
        }

        boolean existed = currentId == null
                ? departmentRepository.existsByCode(code)
                : departmentRepository.existsByCodeAndIdNot(code, currentId);
        if (existed) {
            throw new AppException(ErrorCode.VALUE_EXISTED, "Department code already exists");
        }
    }

    private Departments findDepartmentOrThrow(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Department not found"));
    }

    private String buildUniqueUsername(String baseUsername, Long departmentId) {
        String username = sanitizeIdentifier(baseUsername);
        if (!userRepository.existsByUsername(username)) {
            return username;
        }

        return sanitizeIdentifier(username + "_" + departmentId);
    }

    private String buildUniqueEmail(String username, Long departmentId) {
        String email = username + "@iact.local";
        if (!userRepository.existsByEmail(email)) {
            return email;
        }

        return username + "_" + departmentId + "@iact.local";
    }

    private String getDepartmentIdentity(Departments department) {
        if (department.getCode() != null && !department.getCode().isBlank()) {
            return department.getCode();
        }
        return String.valueOf(department.getId());
    }

    private boolean isInternalDepartmentUser(Users user) {
        return user.getKeycloakId() != null
                && user.getKeycloakId().startsWith(LOCAL_DEPARTMENT_KEYCLOAK_PREFIX);
    }

    private String sanitizeIdentifier(String value) {
        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
    }

    private String normalizeCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeNullableText(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
