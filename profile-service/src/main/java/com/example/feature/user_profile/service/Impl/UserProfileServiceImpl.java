package com.example.feature.user_profile.service.Impl;

import com.example.feature.classes.model.Clazzes;
import com.example.feature.classes.repository.ClassRepository;
import com.example.feature.departments.model.Departments;
import com.example.feature.departments.repository.DepartmentRepository;
import com.example.feature.user_profile.dto.CreateProfileDto;
import com.example.feature.user_profile.dto.ProfileDto;
import com.example.feature.user_profile.dto.UserUpdateRequest;
import com.example.feature.user_profile.mapper.UserProfileMapper;
import com.example.feature.user_profile.model.DepartmentProfile;
import com.example.feature.user_profile.model.StudentProfile;
import com.example.feature.user_profile.repository.DepartmentProfileRepository;
import com.example.feature.user_profile.repository.StudentProfileRepository;
import com.example.feature.user_profile.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

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
            Clazzes clazzProxy = null;
            if (dto.getClassId() != null) {
                clazzProxy = new Clazzes();
                clazzProxy.setId(dto.getClassId());
            }

            StudentProfile student = StudentProfile.builder()
                    .userId(dto.getUserId())
                    .fullName(dto.getFullName())
                    .studentCode(dto.getStudentCode() != null ? dto.getStudentCode() : "N/A_" + dto.getUserId())
                    .clazz(clazzProxy)
                    .build();

            studentRepo.save(student);

        } else if (dto.getRoleType() == 2) {
            // 1. Lưu vào bảng Departments
            Departments newDept = new Departments();
            newDept.setName(dto.getFullName());
            newDept.setDescription(dto.getDescription());
            newDept = departmentsRepo.save(newDept);

            // 2. Lưu vào bảng Department_Profiles
            DepartmentProfile deptProfile = DepartmentProfile.builder()
                    .userId(dto.getUserId())
                    .department(newDept)
                    .fullName(dto.getFullName())
                    .build();
            departmentProfileRepo.save(deptProfile);
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
    public Map<Long, ProfileDto> getProfilesBatch(List<Long> userIds) {
        Map<Long, ProfileDto> resultMap = new HashMap<>();

        List<StudentProfile> students = studentRepo.findAllById(userIds);
        for (StudentProfile s : students) {
            resultMap.put(s.getUserId(), profileMapper.toDto(s));
        }

        List<DepartmentProfile> depts = departmentProfileRepo.findAllById(userIds);
        for (DepartmentProfile d : depts) {
            resultMap.put(d.getUserId(), profileMapper.toDto(d));
        }

        return resultMap;
    }

    @Override
    public List<Long> searchUserIds(String keyword, Long departmentId, Integer roleType, Long classId) {
        if (roleType != null) {
            if (roleType == 1) return studentRepo.searchIdsByCriteria(keyword, departmentId, classId);
            if (roleType == 2) return departmentProfileRepo.searchIdsByCriteria(keyword, departmentId);
        }
        // Nếu không có roleType thì mới tìm cả hai
        Set<Long> resultIds = new HashSet<>();
        resultIds.addAll(studentRepo.searchIdsByCriteria(keyword, departmentId, classId));
        resultIds.addAll(departmentProfileRepo.searchIdsByCriteria(keyword, departmentId));
        return new ArrayList<>(resultIds);
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
            return;
        }

        Optional<DepartmentProfile> deptOpt = departmentProfileRepo.findById(userId);
        if (deptOpt.isPresent()) {
            DepartmentProfile d = deptOpt.get();
            profileMapper.updateDepartment(request, d);
            departmentProfileRepo.save(d);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void createProfilesBatch(List<CreateProfileDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return;
        }
        List<StudentProfile> profilesToSave = dtos.stream()
                .map(profileMapper::toStudentProfile)
                .toList();

        studentRepo.saveAll(profilesToSave);
    }
}