package com.example.userservice.feature.major.service.Impl;

import com.example.dto.PageDTO;
import com.example.exception.AppException;
import com.example.exception.ErrorCode;
import com.example.userservice.feature.classes.repository.ClassRepository;
import com.example.userservice.feature.departments.model.Departments;
import com.example.userservice.feature.departments.repository.DepartmentRepository;
import com.example.userservice.feature.major.dto.MajorRequest;
import com.example.userservice.feature.major.dto.MajorResponse;
import com.example.userservice.feature.major.mapper.MajorMapper;
import com.example.userservice.feature.major.model.Major;
import com.example.userservice.feature.major.repository.MajorRepository;
import com.example.userservice.feature.major.service.MajorService;
import jakarta.persistence.criteria.JoinType;
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
public class MajorServiceImpl implements MajorService {

    private final MajorRepository majorRepository;
    private final DepartmentRepository departmentRepository;
    private final ClassRepository classRepository;
    private final MajorMapper majorMapper;

    @Override
    @Transactional(readOnly = true)
    public List<MajorResponse> getMajors(Long departmentId, Boolean active) {
        List<Major> majors;
        if (departmentId != null && active != null) {
            majors = majorRepository.findByDepartmentIdAndIsActive(departmentId, active);
        } else if (departmentId != null) {
            majors = majorRepository.findByDepartmentId(departmentId);
        } else if (active != null) {
            majors = majorRepository.findByIsActive(active);
        } else {
            majors = majorRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
        }

        return majors.stream()
                .map(majorMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PageDTO<MajorResponse> getMajorPage(
            int page,
            int size,
            String keyword,
            Long departmentId,
            Boolean active,
            String programType) {
        Pageable pageable = PageRequest.of(
                page > 0 ? page - 1 : 0,
                size,
                Sort.by(Sort.Direction.DESC, "id"));

        Page<Major> majorPage = majorRepository.findAll(
                buildSpecification(keyword, departmentId, active, programType),
                pageable);

        List<MajorResponse> data = majorPage.getContent().stream()
                .map(majorMapper::toResponse)
                .collect(Collectors.toList());

        return new PageDTO<>(majorPage, data);
    }

    @Override
    @Transactional(readOnly = true)
    public MajorResponse getMajorById(Long id) {
        return majorMapper.toResponse(findMajorOrThrow(id));
    }

    @Override
    @Transactional
    public MajorResponse createMajor(MajorRequest request) {
        Departments department = findActiveDepartmentOrThrow(request.getDepartmentId());
        validateCodeUnique(request.getCode(), null);
        validateNameUnique(request.getName(), request.getDepartmentId(), null);

        Major major = new Major();
        applyRequest(major, request, department);

        return majorMapper.toResponse(majorRepository.save(major));
    }

    @Override
    @Transactional
    public MajorResponse updateMajor(Long id, MajorRequest request) {
        Major major = findMajorOrThrow(id);
        Departments department = findActiveDepartmentOrThrow(request.getDepartmentId());
        validateCodeUnique(request.getCode(), id);
        validateNameUnique(request.getName(), request.getDepartmentId(), id);
        applyRequest(major, request, department);

        return majorMapper.toResponse(majorRepository.save(major));
    }

    @Override
    @Transactional
    public MajorResponse activateMajor(Long id) {
        Major major = findMajorOrThrow(id);
        if (major.getDepartment() == null || Boolean.FALSE.equals(major.getDepartment().getIsActive())) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Major department is inactive");
        }
        major.setIsActive(true);
        return majorMapper.toResponse(majorRepository.save(major));
    }

    @Override
    @Transactional
    public MajorResponse deactivateMajor(Long id) {
        Major major = findMajorOrThrow(id);
        major.setIsActive(false);
        return majorMapper.toResponse(majorRepository.save(major));
    }

    @Override
    @Transactional
    public void deleteMajor(Long id) {
        Major major = findMajorOrThrow(id);
        if (classRepository.existsByMajorId(id)) {
            major.setIsActive(false);
            majorRepository.save(major);
            return;
        }

        majorRepository.delete(major);
    }

    private Specification<Major> buildSpecification(
            String keyword,
            Long departmentId,
            Boolean active,
            String programType) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (departmentId != null) {
                predicates.add(cb.equal(root.get("department").get("id"), departmentId));
            }

            if (active != null) {
                predicates.add(cb.equal(root.get("isActive"), active));
            }

            if (programType != null && !programType.trim().isEmpty()) {
                predicates.add(cb.equal(
                        cb.lower(root.get("programType")),
                        programType.trim().toLowerCase(Locale.ROOT)));
            }

            if (keyword != null && !keyword.trim().isEmpty()) {
                String likeKeyword = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), likeKeyword),
                        cb.like(cb.lower(root.get("code")), likeKeyword),
                        cb.like(cb.lower(root.get("programType")), likeKeyword),
                        cb.like(cb.lower(root.join("department", JoinType.LEFT).get("name")), likeKeyword)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private void applyRequest(Major major, MajorRequest request, Departments department) {
        major.setName(request.getName().trim());
        major.setCode(normalizeCode(request.getCode()));
        major.setProgramType(normalizeNullableText(request.getProgramType()));
        major.setIsActive(request.getIsActive() == null ? Boolean.TRUE : request.getIsActive());
        major.setDepartment(department);
    }

    private void validateCodeUnique(String rawCode, Long currentId) {
        String code = normalizeCode(rawCode);
        if (code == null) {
            return;
        }

        boolean existed = currentId == null
                ? majorRepository.existsByCode(code)
                : majorRepository.existsByCodeAndIdNot(code, currentId);
        if (existed) {
            throw new AppException(ErrorCode.VALUE_EXISTED, "Major code already exists");
        }
    }

    private void validateNameUnique(String name, Long departmentId, Long currentId) {
        String normalizedName = name.trim();
        boolean existed = currentId == null
                ? majorRepository.existsByNameIgnoreCaseAndDepartmentId(normalizedName, departmentId)
                : majorRepository.existsByNameIgnoreCaseAndDepartmentIdAndIdNot(normalizedName, departmentId,
                        currentId);
        if (existed) {
            throw new AppException(ErrorCode.VALUE_EXISTED, "Major already exists in this department");
        }
    }

    private Major findMajorOrThrow(Long id) {
        return majorRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Major not found"));
    }

    private Departments findActiveDepartmentOrThrow(Long departmentId) {
        Departments department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Department not found"));
        if (Boolean.FALSE.equals(department.getIsActive())) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Department is inactive");
        }
        return department;
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
