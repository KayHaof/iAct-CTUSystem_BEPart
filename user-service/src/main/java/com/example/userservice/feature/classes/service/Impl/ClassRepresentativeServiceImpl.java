package com.example.userservice.feature.classes.service.Impl;

import com.example.exception.AppException;
import com.example.exception.ErrorCode;
import com.example.userservice.feature.classes.dto.ClassRepresentativeRequest;
import com.example.userservice.feature.classes.dto.RepresentativeActivityPermissionResponse;
import com.example.userservice.feature.classes.model.ClassRepresentative;
import com.example.userservice.feature.classes.model.Clazzes;
import com.example.userservice.feature.classes.repository.ClassRepresentativeRepository;
import com.example.userservice.feature.classes.repository.ClassRepository;
import com.example.userservice.feature.classes.service.ClassRepresentativeService;
import com.example.userservice.feature.departments.model.Departments;
import com.example.userservice.feature.user_profile.model.StudentProfile;
import com.example.userservice.feature.user_profile.repository.DepartmentProfileRepository;
import com.example.userservice.feature.user_profile.repository.StudentProfileRepository;
import com.example.userservice.feature.users.model.Users;
import com.example.userservice.feature.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ClassRepresentativeServiceImpl implements ClassRepresentativeService {

    private static final int STUDENT_ROLE_TYPE = 1;
    private static final int DEPARTMENT_ROLE_TYPE = 2;

    private final UserRepository userRepository;
    private final ClassRepository classRepository;
    private final DepartmentProfileRepository departmentProfileRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final ClassRepresentativeRepository representativeRepository;

    @Override
    @Transactional(readOnly = true)
    public RepresentativeActivityPermissionResponse getCurrentStudentActivityPermission() {
        Users currentUser = getCurrentUser();
        if (!Integer.valueOf(STUDENT_ROLE_TYPE).equals(currentUser.getRoleType())) {
            return RepresentativeActivityPermissionResponse.builder()
                    .studentId(currentUser.getId())
                    .canCreateActivity(false)
                    .build();
        }

        Optional<ClassRepresentative> representative = findActiveRepresentative(currentUser.getId());
        if (representative.isEmpty()) {
            return RepresentativeActivityPermissionResponse.builder()
                    .studentId(currentUser.getId())
                    .canCreateActivity(false)
                    .build();
        }

        ClassRepresentative found = representative.get();
        RepresentativeActivityPermissionResponse response = toResponse(found);
        response.setCanCreateActivity(true);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RepresentativeActivityPermissionResponse> getRepresentatives(
            Long departmentId,
            Long classId,
            Boolean active,
            String keyword) {
        Users currentUser = getCurrentUser();
        Long actualDepartmentId = resolveAllowedDepartmentId(currentUser, departmentId);
        return representativeRepository.searchRepresentatives(actualDepartmentId, classId, active, keyword)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public RepresentativeActivityPermissionResponse createRepresentative(ClassRepresentativeRequest request) {
        Users currentUser = getCurrentUser();
        Clazzes clazz = classRepository.findById(request.getClassId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Không tìm thấy lớp"));
        validateClassManageable(currentUser, clazz);

        Users student = userRepository.findById(request.getStudentId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED, "Không tìm thấy sinh viên"));
        if (!Integer.valueOf(STUDENT_ROLE_TYPE).equals(student.getRoleType())) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Người được gán phải là sinh viên.");
        }

        StudentProfile profile = studentProfileRepository.findById(student.getId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED,
                        "Sinh viên chưa có hồ sơ lớp."));
        if (profile.getClazz() == null || !Objects.equals(profile.getClazz().getId(), clazz.getId())) {
            throw new AppException(ErrorCode.INVALID_ACTION,
                    "Sinh viên phải thuộc lớp được gán đại diện.");
        }

        ClassRepresentative representative = new ClassRepresentative();
        representative.setClazz(clazz);
        representative.setStudent(student);
        representative.setRepresentativeType(
                request.getRepresentativeType() != null && !request.getRepresentativeType().isBlank()
                        ? request.getRepresentativeType().trim()
                        : "CLASS_REPRESENTATIVE");
        representative.setStartDate(request.getStartDate());
        representative.setEndDate(request.getEndDate());
        representative.setIsActive(true);
        representative.setAssignedBy(currentUser);

        return toResponse(representativeRepository.save(representative));
    }

    @Override
    @Transactional
    public RepresentativeActivityPermissionResponse deactivateRepresentative(Long id) {
        Users currentUser = getCurrentUser();
        ClassRepresentative representative = representativeRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED,
                        "Không tìm thấy đại diện lớp"));
        validateClassManageable(currentUser, representative.getClazz());
        representative.setIsActive(false);
        if (representative.getEndDate() == null || representative.getEndDate().isAfter(LocalDate.now())) {
            representative.setEndDate(LocalDate.now());
        }
        return toResponse(representativeRepository.save(representative));
    }

    private Users getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return userRepository.findByKeycloakId(authentication.getName())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED, "Không tìm thấy người dùng hiện tại"));
    }

    private Optional<ClassRepresentative> findActiveRepresentative(Long studentId) {
        LocalDate today = LocalDate.now();
        List<ClassRepresentative> representatives = representativeRepository.findActiveByStudentId(studentId, today);
        return representatives.stream().findFirst();
    }

    private Long resolveAllowedDepartmentId(Users currentUser, Long requestedDepartmentId) {
        if (Integer.valueOf(DEPARTMENT_ROLE_TYPE).equals(currentUser.getRoleType())) {
            return currentDepartmentId(currentUser);
        }
        throw new AppException(ErrorCode.FORBIDDEN, "Bạn không có quyền quản lý đại diện lớp.");
    }

    private void validateClassManageable(Users currentUser, Clazzes clazz) {
        if (clazz == null) {
            throw new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Không tìm thấy lớp");
        }
        Departments department = clazz.getMajor() != null ? clazz.getMajor().getDepartment() : null;
        if (!Integer.valueOf(DEPARTMENT_ROLE_TYPE).equals(currentUser.getRoleType())
                || department == null
                || currentDepartmentId(currentUser) == null
                || !Objects.equals(currentDepartmentId(currentUser), department.getId())) {
            throw new AppException(ErrorCode.FORBIDDEN,
                    "Đơn vị chỉ quản lý đại diện của lớp thuộc khoa/viện mình.");
        }
    }

    private Long currentDepartmentId(Users currentUser) {
        return departmentProfileRepository.findById(currentUser.getId())
                .map(profile -> profile.getDepartment() != null ? profile.getDepartment().getId() : null)
                .orElse(null);
    }

    private RepresentativeActivityPermissionResponse toResponse(ClassRepresentative representative) {
        Clazzes clazz = representative.getClazz();
        Departments department = clazz != null && clazz.getMajor() != null
                ? clazz.getMajor().getDepartment()
                : null;
        Users student = representative.getStudent();
        StudentProfile profile = student != null
                ? studentProfileRepository.findById(student.getId()).orElse(null)
                : null;

        return RepresentativeActivityPermissionResponse.builder()
                .id(representative.getId())
                .studentId(student != null ? student.getId() : null)
                .studentCode(profile != null ? profile.getStudentCode() : null)
                .studentName(profile != null && profile.getFullName() != null
                        ? profile.getFullName()
                        : student != null ? student.getUsername() : null)
                .classId(clazz != null ? clazz.getId() : null)
                .classCode(clazz != null ? clazz.getClassCode() : null)
                .className(clazz != null ? clazz.getName() : null)
                .departmentId(department != null ? department.getId() : null)
                .departmentName(department != null ? department.getName() : null)
                .representativeType(representative.getRepresentativeType())
                .isActive(Boolean.TRUE.equals(representative.getIsActive()))
                .canCreateActivity(Boolean.TRUE.equals(representative.getIsActive()))
                .build();
    }
}
