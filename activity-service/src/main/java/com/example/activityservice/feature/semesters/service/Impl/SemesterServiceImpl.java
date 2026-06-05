package com.example.activityservice.feature.semesters.service.impl;

import com.example.activityservice.feature.activities.repository.ActivityRepository;
import com.example.activityservice.feature.semesters.dto.SemesterRequest;
import com.example.activityservice.feature.semesters.dto.SemesterResponse;
import com.example.activityservice.feature.semesters.mapper.SemesterMapper;
import com.example.activityservice.feature.semesters.model.Semesters;
import com.example.activityservice.feature.semesters.repository.SemesterRepository;
import com.example.activityservice.feature.semesters.service.SemesterService;
import com.example.exception.AppException;
import com.example.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SemesterServiceImpl implements SemesterService {

    private final SemesterRepository semesterRepository;
    private final ActivityRepository activityRepository;
    private final SemesterMapper semesterMapper;

    @Override
    @Transactional
    public SemesterResponse createSemester(SemesterRequest request) {
        String name = resolveName(request);
        log.info("Creating semester: {} - {}", name, request.getAcademicYear());
        validateRequiredFields(name, request.getAcademicYear());
        validateDates(request);
        validateUniqueSemester(name, request.getAcademicYear(), null);

        if (Boolean.TRUE.equals(request.getIsActive())) {
            semesterRepository.deactivateAllSemesters();
        }

        Semesters semester = semesterMapper.toEntity(request);
        return semesterMapper.toResponse(semesterRepository.save(semester));
    }

    @Override
    public List<SemesterResponse> getAllSemesters(Boolean active, Boolean locked, String academicYear) {
        return findSemesters(active, locked, academicYear).stream()
                .map(semesterMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public SemesterResponse getSemesterById(Long id) {
        return semesterMapper.toResponse(findSemesterOrThrow(id));
    }

    @Override
    public SemesterResponse getActiveSemester() {
        Semesters semester = semesterRepository.findByIsActiveTrue()
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Chưa có học kỳ nào đang mở!"));
        return semesterMapper.toResponse(semester);
    }

    @Override
    @Transactional
    public SemesterResponse updateSemester(Long id, SemesterRequest request) {
        log.info("Updating semester ID: {}", id);
        Semesters semester = findSemesterOrThrow(id);

        if (Boolean.TRUE.equals(semester.getIsLocked())) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Không thể cập nhật học kỳ đã bị khóa!");
        }

        String name = resolveName(request);
        validateRequiredFields(name, request.getAcademicYear());
        validateDates(request);
        validateUniqueSemester(name, request.getAcademicYear(), id);

        if (Boolean.TRUE.equals(request.getIsActive()) && !Boolean.TRUE.equals(semester.getIsActive())) {
            semesterRepository.deactivateAllSemesters();
        }

        semesterMapper.updateEntityFromRequest(request, semester);
        return semesterMapper.toResponse(semesterRepository.save(semester));
    }

    @Override
    @Transactional
    public SemesterResponse activateSemester(Long id) {
        Semesters semester = findSemesterOrThrow(id);
        semesterRepository.deactivateAllSemesters();
        semester.setIsActive(true);
        return semesterMapper.toResponse(semesterRepository.save(semester));
    }

    @Override
    @Transactional
    public SemesterResponse lockSemester(Long id) {
        Semesters semester = findSemesterOrThrow(id);
        semester.setIsLocked(true);
        return semesterMapper.toResponse(semesterRepository.save(semester));
    }

    @Override
    @Transactional
    public SemesterResponse unlockSemester(Long id) {
        Semesters semester = findSemesterOrThrow(id);
        semester.setIsLocked(false);
        return semesterMapper.toResponse(semesterRepository.save(semester));
    }

    @Override
    @Transactional
    public void deleteSemester(Long id) {
        log.info("Deleting semester ID: {}", id);
        Semesters semester = findSemesterOrThrow(id);

        if (Boolean.TRUE.equals(semester.getIsLocked())) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Không thể xóa học kỳ đã bị khóa!");
        }

        if (activityRepository.existsBySemesterId(id)) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Không thể xóa học kỳ đang được hoạt động sử dụng!");
        }

        semesterRepository.deleteById(id);
    }

    private void validateDates(SemesterRequest request) {
        if (request.getStartDate() != null
                && request.getEndDate() != null
                && request.getStartDate().isAfter(request.getEndDate())) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Ngày bắt đầu phải trước hoặc bằng ngày kết thúc!");
        }
    }

    private void validateRequiredFields(String name, String academicYear) {
        if (isBlank(name)) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Tên học kỳ không được để trống!");
        }
        if (isBlank(academicYear)) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Năm học không được để trống!");
        }
    }

    private void validateUniqueSemester(String name, String academicYear, Long currentId) {
        boolean existed = currentId == null
                ? semesterRepository.existsByNameAndAcademicYear(name, academicYear)
                : semesterRepository.existsByNameAndAcademicYearAndIdNot(name, academicYear, currentId);
        if (existed) {
            throw new AppException(ErrorCode.VALUE_EXISTED, "Học kỳ đã tồn tại trong năm học này!");
        }
    }

    private List<Semesters> findSemesters(Boolean active, Boolean locked, String academicYear) {
        boolean hasYear = !isBlank(academicYear);
        if (active != null && locked != null && hasYear) {
            return semesterRepository.findByIsActiveAndIsLockedAndAcademicYear(active, locked, academicYear);
        }
        if (active != null && locked != null) {
            return semesterRepository.findByIsActiveAndIsLocked(active, locked);
        }
        if (active != null && hasYear) {
            return semesterRepository.findByIsActiveAndAcademicYear(active, academicYear);
        }
        if (locked != null && hasYear) {
            return semesterRepository.findByIsLockedAndAcademicYear(locked, academicYear);
        }
        if (active != null) {
            return semesterRepository.findByIsActive(active);
        }
        if (locked != null) {
            return semesterRepository.findByIsLocked(locked);
        }
        if (hasYear) {
            return semesterRepository.findByAcademicYear(academicYear);
        }
        return semesterRepository.findAll();
    }

    private Semesters findSemesterOrThrow(Long id) {
        return semesterRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Không tìm thấy học kỳ!"));
    }

    private String resolveName(SemesterRequest request) {
        return request.getName() != null ? request.getName() : request.getSemesterName();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
