package com.example.feature.major.service.Impl;

import com.example.exception.AppException;
import com.example.exception.ErrorCode;
import com.example.feature.departments.model.Departments;
import com.example.feature.departments.repository.DepartmentRepository;
import com.example.feature.major.dto.MajorRequest;
import com.example.feature.major.dto.MajorResponse;
import com.example.feature.major.mapper.MajorMapper;
import com.example.feature.major.model.Major;
import com.example.feature.major.repository.MajorRepository;
import com.example.feature.major.service.MajorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MajorServiceImpl implements MajorService {

    private final MajorRepository majorRepository;
    private final DepartmentRepository departmentRepository;
    private final MajorMapper majorMapper;

    @Override
    @Transactional(readOnly = true)
    public List<MajorResponse> getMajors(Long departmentId) {
        List<Major> majors;
        if (departmentId != null) {
            majors = majorRepository.findByDepartmentId(departmentId);
        } else {
            majors = majorRepository.findAll();
        }
        return majors.stream().map(majorMapper::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public MajorResponse createMajor(MajorRequest request) {
        Departments dept = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Không tìm thấy Khoa/Đơn vị"));

        if (majorRepository.existsByNameAndDepartmentId(request.getName(), request.getDepartmentId())) {
            throw new AppException(ErrorCode.VALUE_EXISTED, "Chuyên ngành này đã tồn tại trong Khoa");
        }

        Major major = majorMapper.toEntity(request);
        major.setDepartment(dept); // Gắn quan hệ với bảng Khoa

        return majorMapper.toResponse(majorRepository.save(major));
    }

    @Override
    @Transactional
    public MajorResponse updateMajor(Long id, MajorRequest request) {
        Major major = majorRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Không tìm thấy Chuyên ngành"));

        Departments dept = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Không tìm thấy Khoa/Đơn vị"));

        if (!major.getName().equals(request.getName()) &&
                majorRepository.existsByNameAndDepartmentId(request.getName(), request.getDepartmentId())) {
            throw new AppException(ErrorCode.VALUE_EXISTED, "Chuyên ngành này đã tồn tại trong Khoa");
        }

        majorMapper.updateEntity(major, request);
        major.setDepartment(dept);

        return majorMapper.toResponse(majorRepository.save(major));
    }

    @Override
    @Transactional
    public void deleteMajor(Long id) {
        if (!majorRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy Chuyên ngành");
        }
        majorRepository.deleteById(id);
    }
}