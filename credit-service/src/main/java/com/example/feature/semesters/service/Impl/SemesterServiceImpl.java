package com.example.feature.semesters.service.Impl;

import com.example.exception.AppException;
import com.example.exception.ErrorCode;
import com.example.feature.semesters.dto.SemesterRequest;
import com.example.feature.semesters.dto.SemesterResponse;
import com.example.feature.semesters.mapper.SemesterMapper;
import com.example.feature.semesters.model.Semesters;
import com.example.feature.semesters.repository.SemesterRepository;
import com.example.feature.semesters.service.SemesterService;
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
    private final SemesterMapper semesterMapper;

    @Override
    @Transactional
    public SemesterResponse createSemester(SemesterRequest request) {
        log.info("Tạo học kỳ mới: {} - {}", request.getSemesterName(), request.getAcademicYear());
        validateDates(request);

        if (Boolean.TRUE.equals(request.getIsActive())) {
            semesterRepository.deactivateAllSemesters();
        }

        Semesters semester = semesterMapper.toEntity(request);

        return semesterMapper.toResponse(semesterRepository.save(semester));
    }

    @Override
    public List<SemesterResponse> getAllSemesters() {
        return semesterRepository.findAll().stream()
                .map(semesterMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public SemesterResponse getSemesterById(Long id) {
        Semesters semester = semesterRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Không tìm thấy học kỳ!"));
        return semesterMapper.toResponse(semester);
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
        log.info("Cập nhật học kỳ ID: {}", id);
        Semesters semester = semesterRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Không tìm thấy học kỳ!"));

        validateDates(request);

        if (Boolean.TRUE.equals(request.getIsActive()) && !Boolean.TRUE.equals(semester.getIsActive())) {
            semesterRepository.deactivateAllSemesters();
        }

        semesterMapper.updateEntityFromRequest(request, semester);

        return semesterMapper.toResponse(semesterRepository.save(semester));
    }

    @Override
    @Transactional
    public void deleteSemester(Long id) {
        log.info("Xóa học kỳ ID: {}", id);
        Semesters semester = semesterRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Không tìm thấy học kỳ để xóa!"));

        if (Boolean.TRUE.equals(semester.getIsLocked())) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Không thể xóa học kỳ đã bị khóa!");
        }

        semesterRepository.deleteById(id);
    }

    // --- Helper ---
    private void validateDates(SemesterRequest request) {
        if (request.getStartDate() != null && request.getEndDate() != null) {
            if (request.getStartDate().isAfter(request.getEndDate())) {
                throw new AppException(ErrorCode.INVALID_ACTION, "Ngày bắt đầu phải trước ngày kết thúc!");
            }
        }
    }
}