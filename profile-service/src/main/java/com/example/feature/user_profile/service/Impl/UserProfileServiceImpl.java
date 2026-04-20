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
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
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
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Override
    @Transactional
    public void createProfile(CreateProfileDto dto) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", dto.getUserId());
        payload.put("fullName", dto.getFullName());

        if (dto.getRoleType() == null || dto.getRoleType() == 1) {
            // ĐÃ SỬA LỖI: Tìm Class thật từ DB để lấy đúng ClassCode gửi đi
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

            payload.put("studentCode", student.getStudentCode());

            // Gửi kèm ClassId và ClassCode sang cho Identity
            if (actualClass != null) {
                payload.put("classId", actualClass.getId());
                payload.put("classCode", actualClass.getClassCode());
            }

        } else if (dto.getRoleType() == 2) {
            Departments newDept = new Departments();
            newDept.setName(dto.getFullName());
            newDept.setDescription(dto.getDescription());
            newDept = departmentsRepo.save(newDept);

            DepartmentProfile deptProfile = DepartmentProfile.builder()
                    .userId(dto.getUserId())
                    .department(newDept)
                    .fullName(dto.getFullName())
                    .build();
            departmentProfileRepo.save(deptProfile);

            payload.put("departmentId", newDept.getId());
            // Gửi kèm Tên Đơn vị sang cho Identity
            payload.put("departmentName", newDept.getName());
        }

        try {
            String jsonString = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send("profile-created-topic", jsonString);
            log.info("Đã bắn Kafka báo Identity thêm IdtLocal cho User ID: {}", dto.getUserId());
        } catch (Exception e) {
            log.error("Lỗi khi parse JSON gửi Kafka khi tạo mới: {}", e.getMessage());
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

            Map<String, Object> payload = new HashMap<>();
            payload.put("userId", userId);
            payload.put("fullName", s.getFullName());
            payload.put("studentCode", s.getStudentCode());
            payload.put("avatarUrl", s.getAvatarUrl());

            // THÊM: Cập nhật luôn classCode
            if (s.getClazz() != null) {
                payload.put("classId", s.getClazz().getId());
                payload.put("classCode", s.getClazz().getClassCode());
            }

            try {
                String jsonString = objectMapper.writeValueAsString(payload);
                kafkaTemplate.send("profile-updated-topic", jsonString);
                log.info("Đã bắn Kafka báo Identity cập nhật Sinh viên ID: {}", userId);
            } catch (Exception e) {
                log.error("Lỗi khi parse JSON gửi Kafka: {}", e.getMessage());
            }
            return;
        }

        Optional<DepartmentProfile> deptOpt = departmentProfileRepo.findById(userId);
        if (deptOpt.isPresent()) {
            DepartmentProfile d = deptOpt.get();
            profileMapper.updateDepartment(request, d);
            departmentProfileRepo.save(d);

            Map<String, Object> payload = new HashMap<>();
            payload.put("userId", userId);
            payload.put("fullName", d.getFullName());
            payload.put("avatarUrl", d.getAvatarUrl());
            // THÊM: Cập nhật luôn departmentName
            payload.put("departmentName", d.getFullName());

            try {
                String jsonString = objectMapper.writeValueAsString(payload);
                kafkaTemplate.send("profile-updated-topic", jsonString);
                log.info("Đã bắn Kafka báo Identity cập nhật Đơn vị ID: {}", userId);
            } catch (Exception e) {
                log.error("Lỗi khi parse JSON gửi Kafka: {}", e.getMessage());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createProfilesBatch(List<CreateProfileDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return;
        }

        // 1. Gom tất cả mã lớp lại để Query DB 1 lần cho lẹ
        Set<String> classCodes = dtos.stream()
                .map(CreateProfileDto::getClassCode)
                .filter(code -> code != null && !code.trim().isEmpty())
                .collect(Collectors.toSet());

        // 2. Tra cứu lấy cục Map (Key: classCode, Value: Clazzes)
        Map<String, Clazzes> classMap = new HashMap<>();
        if (!classCodes.isEmpty()) {
            List<Clazzes> classes = clazzRepo.findByClassCodeIn(classCodes);
            for (Clazzes c : classes) {
                classMap.put(c.getClassCode(), c);
            }
        }

        // 3. Chuẩn bị list data để insert
        List<StudentProfile> profilesToSave = new ArrayList<>();
        for (CreateProfileDto dto : dtos) {
            // Lấy ID thật từ Map dựa vào cái Code
            Clazzes clazz = dto.getClassCode() != null ? classMap.get(dto.getClassCode()) : null;

            StudentProfile student = StudentProfile.builder()
                    .userId(dto.getUserId())
                    .fullName(dto.getFullName())
                    .studentCode(dto.getStudentCode() != null ? dto.getStudentCode() : "N/A_" + dto.getUserId())
                    .clazz(clazz)
                    .build();
            profilesToSave.add(student);
        }

        // 4. Bơm 1 phát xuống DB Profile
        profilesToSave = studentRepo.saveAll(profilesToSave);

        // 5. [VŨ KHÍ BÍ MẬT] Bắn liên thanh Kafka cho Identity Service biết mà tạo IdtLocal
        for (StudentProfile s : profilesToSave) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("userId", s.getUserId());
            payload.put("fullName", s.getFullName());
            payload.put("studentCode", s.getStudentCode());

            // THÊM: Gửi full classId và classCode cho Batch Import
            if (s.getClazz() != null) {
                payload.put("classId", s.getClazz().getId());
                payload.put("classCode", s.getClazz().getClassCode());
            }

            try {
                String jsonString = objectMapper.writeValueAsString(payload);
                kafkaTemplate.send("profile-created-topic", jsonString);
            } catch (Exception e) {
                log.error("Lỗi khi gửi Kafka batch tạo user: {}", e.getMessage());
            }
        }

        log.info("Đã Import và bắn Kafka thành công {} hồ sơ sinh viên.", profilesToSave.size());
    }
}