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
import com.example.userservice.feature.keycloak.service.KeycloakUserProvisionRequest;
import com.example.userservice.feature.keycloak.service.KeycloakUserProvisionService;
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
    private static final String DEPARTMENT_ROLE_NAME = "department";
    private static final String DEPARTMENT_EMAIL_DOMAIN = "@iact.com";
    private static final String DEPARTMENT_DEFAULT_PASSWORD = "Departmentiact123@";

    private final DepartmentRepository departmentRepository;
    private final MajorRepository majorRepository;
    private final DepartmentProfileRepository departmentProfileRepository;
    private final UserRepository userRepository;
    private final DepartmentMapper departmentMapper;
    private final KeycloakUserProvisionService keycloakUserProvisionService;

    @Override
    @Transactional
    public DepartmentResponse createDepartment(DepartmentRequest request) {
        validateCodeUnique(request.getCode(), null);

        Departments department = new Departments();
        applyRequest(department, request);
        Departments savedDepartment = departmentRepository.save(department);

        String createdKeycloakId = null;
        try {
            DepartmentAccountPayload accountPayload = buildDepartmentAccountPayload(savedDepartment);
            createdKeycloakId = keycloakUserProvisionService.createUser(buildKeycloakRequest(savedDepartment, accountPayload));

            Users user = createDepartmentUser(savedDepartment, accountPayload, createdKeycloakId);
            DepartmentProfile profile = buildDepartmentProfile(savedDepartment, user, request);
            departmentProfileRepository.save(profile);

            return toResponse(savedDepartment);
        } catch (Exception exception) {
            if (createdKeycloakId != null) {
                keycloakUserProvisionService.deleteUser(createdKeycloakId);
            }
            throw exception;
        }
    }

    @Override
    @Transactional
    public DepartmentResponse updateDepartment(Long id, DepartmentRequest request) {
        Departments department = findDepartmentOrThrow(id);
        validateCodeUnique(request.getCode(), id);
        applyRequest(department, request);

        Departments savedDepartment = departmentRepository.save(department);
        DepartmentProfile profile = departmentProfileRepository.findFirstByDepartmentId(savedDepartment.getId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Department profile not found"));
        Users user = userRepository.findById(profile.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED, "Department user not found"));

        DepartmentAccountPayload accountPayload = buildDepartmentAccountPayload(savedDepartment);
        validateDepartmentAccountUnique(accountPayload.username(), accountPayload.email(), user.getId());
        keycloakUserProvisionService.updateUser(user.getKeycloakId(), buildKeycloakRequest(savedDepartment, accountPayload));

        user.setUsername(accountPayload.username());
        user.setEmail(accountPayload.email());
        user.setStatus(Boolean.FALSE.equals(savedDepartment.getIsActive()) ? 0 : 1);
        userRepository.save(user);

        updateDepartmentProfile(profile, savedDepartment, request);
        departmentProfileRepository.save(profile);

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

        deleteDepartmentProfiles(id);
        departmentRepository.delete(department);
    }

    @Override
    @Transactional
    public DepartmentResponse activateDepartment(Long id) {
        Departments department = findDepartmentOrThrow(id);
        department.setIsActive(true);
        Departments savedDepartment = departmentRepository.save(department);
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

    private Users createDepartmentUser(Departments department, DepartmentAccountPayload accountPayload, String keycloakId) {
        validateDepartmentAccountUnique(accountPayload.username(), accountPayload.email(), null);
        Users user = Users.builder()
                .keycloakId(keycloakId)
                .username(accountPayload.username())
                .email(accountPayload.email())
                .roleType(ROLE_DEPARTMENT)
                .status(Boolean.FALSE.equals(department.getIsActive()) ? 0 : 1)
                .build();
        return userRepository.save(user);
    }

    private DepartmentProfile buildDepartmentProfile(Departments department, Users user, DepartmentRequest request) {
        return DepartmentProfile.builder()
                .userId(user.getId())
                .department(department)
                .fullName(department.getName())
                .phone(normalizeNullableText(request.getPhone()))
                .address(normalizeNullableText(request.getAddress()))
                .avatarUrl(normalizeNullableText(request.getAvatarUrl()))
                .build();
    }

    private void updateDepartmentProfile(DepartmentProfile profile, Departments department, DepartmentRequest request) {
        profile.setDepartment(department);
        profile.setFullName(department.getName());
        profile.setPhone(normalizeNullableText(request.getPhone()));
        profile.setAddress(normalizeNullableText(request.getAddress()));
        profile.setAvatarUrl(normalizeNullableText(request.getAvatarUrl()));
    }

    private void syncDepartmentUserStatus(Long departmentId, Boolean isActive) {
        departmentProfileRepository.findFirstByDepartmentId(departmentId)
                .flatMap(profile -> userRepository.findById(profile.getUserId()))
                .ifPresent(user -> {
                    user.setStatus(Boolean.FALSE.equals(isActive) ? 0 : 1);
                    keycloakUserProvisionService.updateUserEnabled(user.getKeycloakId(), !Boolean.FALSE.equals(isActive));
                    userRepository.save(user);
                });
    }

    private void deleteDepartmentProfiles(Long departmentId) {
        List<DepartmentProfile> profiles = departmentProfileRepository.findByDepartmentId(departmentId);
        for (DepartmentProfile profile : profiles) {
            userRepository.findById(profile.getUserId()).ifPresent(user -> {
                keycloakUserProvisionService.deleteUser(user.getKeycloakId());
                departmentProfileRepository.delete(profile);
                userRepository.delete(user);
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

    private void validateDepartmentAccountUnique(String username, String email, Long currentUserId) {
        userRepository.findByUsername(username)
                .filter(user -> currentUserId == null || !user.getId().equals(currentUserId))
                .ifPresent(user -> {
                    throw new AppException(ErrorCode.VALUE_EXISTED, "Department username already exists");
                });

        userRepository.findByEmail(email)
                .filter(user -> currentUserId == null || !user.getId().equals(currentUserId))
                .ifPresent(user -> {
                    throw new AppException(ErrorCode.VALUE_EXISTED, "Department email already exists");
                });
    }

    private Departments findDepartmentOrThrow(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Department not found"));
    }

    private DepartmentAccountPayload buildDepartmentAccountPayload(Departments department) {
        String identity = getDepartmentIdentity(department);
        String username = sanitizeIdentifier("dept_" + identity.toLowerCase(Locale.ROOT));
        if (username == null || username.isBlank()) {
            throw new AppException(ErrorCode.INCORRECT_VALUE, "Department code is invalid for account provisioning");
        }
        return new DepartmentAccountPayload(username, username + DEPARTMENT_EMAIL_DOMAIN);
    }

    private KeycloakUserProvisionRequest buildKeycloakRequest(Departments department, DepartmentAccountPayload accountPayload) {
        return KeycloakUserProvisionRequest.builder()
                .username(accountPayload.username())
                .email(accountPayload.email())
                .firstName(department.getName())
                .lastName("")
                .password(DEPARTMENT_DEFAULT_PASSWORD)
                .roleName(DEPARTMENT_ROLE_NAME)
                .enabled(!Boolean.FALSE.equals(department.getIsActive()))
                .build();
    }

    private String getDepartmentIdentity(Departments department) {
        if (department.getCode() != null && !department.getCode().isBlank()) {
            return department.getCode();
        }
        return String.valueOf(department.getId());
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

    private record DepartmentAccountPayload(String username, String email) {
    }
}
