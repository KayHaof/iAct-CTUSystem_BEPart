package com.example.userservice.feature.user_profile.service.Impl;

import com.example.exception.AppException;
import com.example.exception.ErrorCode;
import com.example.userservice.feature.classes.model.Clazzes;
import com.example.userservice.feature.classes.repository.ClassRepository;
import com.example.userservice.feature.departments.model.Departments;
import com.example.userservice.feature.departments.repository.DepartmentRepository;
import com.example.userservice.feature.user_profile.dto.CreateProfileDto;
import com.example.userservice.feature.user_profile.dto.ProfileDto;
import com.example.userservice.feature.user_profile.dto.UserUpdateRequest;
import com.example.userservice.feature.user_profile.mapper.UserProfileMapper;
import com.example.userservice.feature.user_profile.model.DepartmentProfile;
import com.example.userservice.feature.user_profile.model.StudentProfile;
import com.example.userservice.feature.user_profile.repository.DepartmentProfileRepository;
import com.example.userservice.feature.user_profile.repository.StudentProfileRepository;
import com.example.userservice.feature.user_profile.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileServiceImpl implements UserProfileService {

    private final StudentProfileRepository studentRepo;
    private final DepartmentProfileRepository departmentProfileRepo;
    private final DepartmentRepository departmentsRepo;
    private final ClassRepository clazzRepo;
    private final UserProfileMapper profileMapper;

    @Override
    @Transactional
    public void createProfile(CreateProfileDto dto) {
        if (dto.getRoleType() == null || dto.getRoleType() == 1) {
            Clazzes actualClass = null;
            if (dto.getClassId() != null) {
                actualClass = clazzRepo.findById(dto.getClassId()).orElse(null);
            }

            StudentProfile student = StudentProfile.builder()
                    .userId(dto.getUserId())
                    .fullName(dto.getFullName())
                    .studentCode(dto.getStudentCode() != null ? dto.getStudentCode() : "N/A_" + dto.getUserId())
                    .clazz(actualClass)
                    .build();

            studentRepo.save(student);
            log.info("Đã tạo StudentProfile cho User ID: {}", dto.getUserId());

        } else if (dto.getRoleType() == 2) {
            Departments department = resolveDepartmentForProfile(dto);

            DepartmentProfile deptProfile = DepartmentProfile.builder()
                    .userId(dto.getUserId())
                    .department(department)
                    .fullName(dto.getFullName())
                    .phone(dto.getPhone())
                    .address(dto.getAddress())
                    .avatarUrl(dto.getAvatarUrl())
                    .build();
            departmentProfileRepo.save(deptProfile);
            log.info("Đã tạo DepartmentProfile cho User ID: {}", dto.getUserId());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileDto getProfileByUserId(Long userId) {
        Optional<StudentProfile> studentOpt = studentRepo.findById(userId);
        if (studentOpt.isPresent()) {
            return profileMapper.toDto(studentOpt.get());
        }

        Optional<DepartmentProfile> deptOpt = departmentProfileRepo.findById(userId);
        return deptOpt.map(profileMapper::toDto).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, ProfileDto> getProfilesBatch(List<Long> userIds) {
        Map<Long, ProfileDto> resultMap = new HashMap<>();
        if (userIds == null || userIds.isEmpty())
            return resultMap;

        List<StudentProfile> students = studentRepo.findAllById(userIds);
        for (StudentProfile s : students) {
            resultMap.put(s.getUserId(), profileMapper.toDto(s));
        }

        List<DepartmentProfile> depts = departmentProfileRepo.findByUserIdIn(userIds);
        for (DepartmentProfile d : depts) {
            resultMap.put(d.getUserId(), profileMapper.toDto(d));
        }

        return resultMap;
    }

    @Override
    public Set<Long> searchUserIds(String keyword, Long departmentId, Integer roleType, Long classId) {
        Set<Long> resultIds = new HashSet<>();
        if (roleType != null) {
            if (roleType == 1)
                return new HashSet<>(studentRepo.searchIdsByCriteria(keyword, departmentId, classId));
            if (roleType == 2)
                return new HashSet<>(departmentProfileRepo.searchIdsByCriteria(keyword, departmentId));
        }

        resultIds.addAll(studentRepo.searchIdsByCriteria(keyword, departmentId, classId));
        resultIds.addAll(departmentProfileRepo.searchIdsByCriteria(keyword, departmentId));
        return resultIds;
    }

    @Override
    @Transactional
    public void updateUserProfile(Long userId, UserUpdateRequest request) {
        Optional<StudentProfile> studentOpt = studentRepo.findById(userId);
        if (studentOpt.isPresent()) {
            StudentProfile s = studentOpt.get();
            profileMapper.updateStudent(request, s);

            if (request.getClassId() != null) {
                Clazzes clazz = clazzRepo.findById(request.getClassId()).orElse(null);
                s.setClazz(clazz);
            }
            studentRepo.save(s);
            log.info("Đã cập nhật StudentProfile cho User ID: {}", userId);
            return;
        }

        Optional<DepartmentProfile> deptOpt = departmentProfileRepo.findById(userId);
        if (deptOpt.isPresent()) {
            DepartmentProfile d = deptOpt.get();
            profileMapper.updateDepartment(request, d);
            if (request.getDepartmentId() != null) {
                Departments department = departmentsRepo.findById(request.getDepartmentId())
                        .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Department not found"));
                d.setDepartment(department);
            }
            departmentProfileRepo.save(d);
            log.info("Đã cập nhật DepartmentProfile cho User ID: {}", userId);
        }
    }

    private Departments resolveDepartmentForProfile(CreateProfileDto dto) {
        if (dto.getDepartmentId() != null) {
            return departmentsRepo.findById(dto.getDepartmentId())
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Department not found"));
        }

        Departments department = new Departments();
        department.setName(dto.getFullName());
        department.setDescription(dto.getDescription());
        return departmentsRepo.save(department);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createProfilesBatch(List<CreateProfileDto> dtos) {
        if (dtos == null || dtos.isEmpty())
            return;

        Set<String> classCodes = dtos.stream()
                .map(CreateProfileDto::getClassCode)
                .filter(code -> code != null && !code.trim().isEmpty())
                .collect(Collectors.toSet());

        Map<String, Clazzes> classMap = new HashMap<>();
        if (!classCodes.isEmpty()) {
            List<Clazzes> classes = clazzRepo.findByClassCodeIn(classCodes);
            for (Clazzes c : classes) {
                classMap.put(c.getClassCode(), c);
            }
        }

        List<StudentProfile> profilesToSave = new ArrayList<>();
        for (CreateProfileDto dto : dtos) {
            Clazzes clazz = dto.getClassCode() != null ? classMap.get(dto.getClassCode()) : null;

            StudentProfile student = StudentProfile.builder()
                    .userId(dto.getUserId())
                    .fullName(dto.getFullName())
                    .studentCode(dto.getStudentCode() != null ? dto.getStudentCode() : "N/A_" + dto.getUserId())
                    .clazz(clazz)
                    .build();
            profilesToSave.add(student);
        }

        studentRepo.saveAll(profilesToSave);
        log.info("Đã Import thành công {} hồ sơ sinh viên.", profilesToSave.size());
    }

    // THỰC THI HÀM CHECK MSSV KHI IMPORT
    @Override
    public Set<String> checkExistingStudentCodes(Set<String> studentCodes) {
        if (studentCodes == null || studentCodes.isEmpty()) {
            return new HashSet<>();
        }
        // Gọi xuống Repository để check
        return studentRepo.findExistingStudentCodes(studentCodes);
    }
}
