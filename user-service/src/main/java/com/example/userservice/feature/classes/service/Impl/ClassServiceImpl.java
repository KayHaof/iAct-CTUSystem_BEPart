package com.example.userservice.feature.classes.service.Impl;

import com.example.dto.PageDTO;
import com.example.exception.AppException;
import com.example.exception.ErrorCode;
import com.example.userservice.feature.classes.dto.ClassRequest;
import com.example.userservice.feature.classes.dto.ClassResponse;
import com.example.userservice.feature.classes.mapper.ClassMapper;
import com.example.userservice.feature.classes.model.Clazzes;
import com.example.userservice.feature.classes.repository.ClassRepository;
import com.example.userservice.feature.classes.service.ClassService;
import com.example.userservice.feature.major.model.Major;
import com.example.userservice.feature.major.repository.MajorRepository;
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
public class ClassServiceImpl implements ClassService {
    private final ClassRepository classRepository;
    private final MajorRepository majorRepository;
    private final ClassMapper classMapper;

    @Override
    public List<Long> getClassIdsByDepartment(Long departmentId) {
        return classRepository.findClassIdsByDepartmentId(departmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassResponse> getClassesByMajor(Long majorId, String academicYear) {
        List<Clazzes> classes;
        if (majorId != null && academicYear != null && !academicYear.trim().isEmpty()) {
            classes = classRepository.findByMajorIdAndAcademicYear(majorId, academicYear);
        } else if (majorId != null) {
            classes = classRepository.findByMajorId(majorId);
        } else {
            classes = classRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
        }
        return classes.stream().map(classMapper::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PageDTO<ClassResponse> getClassPage(
            int page,
            int size,
            String keyword,
            Long departmentId,
            Long majorId,
            String academicYear,
            Boolean active) {
        Pageable pageable = PageRequest.of(
                page > 0 ? page - 1 : 0,
                size,
                Sort.by(Sort.Direction.DESC, "id"));

        Page<Clazzes> classPage = classRepository.findAll(
                buildSpecification(keyword, departmentId, majorId, academicYear, active),
                pageable);

        List<ClassResponse> data = classPage.getContent().stream()
                .map(classMapper::toResponse)
                .collect(Collectors.toList());

        return new PageDTO<>(classPage, data);
    }

    @Override
    @Transactional(readOnly = true)
    public ClassResponse getClassById(Long id) {
        Clazzes clazz = findClassOrThrow(id);
        return classMapper.toResponse(clazz);
    }

    @Override
    @Transactional
    public ClassResponse createClass(ClassRequest request) {
        validateClassCodeUnique(request.getClassCode(), null);

        Major major = findMajorOrThrow(request.getMajorId());

        Clazzes clazz = new Clazzes();
        applyRequest(clazz, request, major);

        return classMapper.toResponse(classRepository.save(clazz));
    }

    @Override
    @Transactional
    public ClassResponse updateClass(Long id, ClassRequest request) {
        Clazzes clazz = findClassOrThrow(id);

        if (!clazz.getClassCode().equalsIgnoreCase(request.getClassCode().trim())) {
            validateClassCodeUnique(request.getClassCode(), id);
        }

        Major major = findMajorOrThrow(request.getMajorId());
        applyRequest(clazz, request, major);

        return classMapper.toResponse(classRepository.save(clazz));
    }

    @Override
    @Transactional
    public ClassResponse activateClass(Long id) {
        Clazzes clazz = findClassOrThrow(id);
        clazz.setIsActive(true);
        return classMapper.toResponse(classRepository.save(clazz));
    }

    @Override
    @Transactional
    public ClassResponse deactivateClass(Long id) {
        Clazzes clazz = findClassOrThrow(id);
        clazz.setIsActive(false);
        return classMapper.toResponse(classRepository.save(clazz));
    }

    @Override
    @Transactional
    public void deleteClass(Long id) {
        Clazzes clazz = findClassOrThrow(id);
        classRepository.delete(clazz);
    }

    private Specification<Clazzes> buildSpecification(
            String keyword,
            Long departmentId,
            Long majorId,
            String academicYear,
            Boolean active) {
        return (root, query, cb) -> {
            query.distinct(true);
            List<Predicate> predicates = new ArrayList<>();

            if (departmentId != null) {
                predicates.add(cb.equal(root.get("major").get("department").get("id"), departmentId));
            }

            if (majorId != null) {
                predicates.add(cb.equal(root.get("major").get("id"), majorId));
            }

            if (active != null) {
                predicates.add(cb.equal(root.get("isActive"), active));
            }

            if (academicYear != null && !academicYear.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("academicYear"), academicYear.trim()));
            }

            if (keyword != null && !keyword.trim().isEmpty()) {
                String likeKeyword = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), likeKeyword),
                        cb.like(cb.lower(root.get("classCode")), likeKeyword),
                        cb.like(cb.lower(root.get("academicYear")), likeKeyword)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private void applyRequest(Clazzes clazz, ClassRequest request, Major major) {
        clazz.setClassCode(normalizeCode(request.getClassCode()));
        clazz.setName(request.getName().trim());
        clazz.setAcademicYear(normalizeNullableText(request.getAcademicYear()));
        clazz.setMajor(major);
        clazz.setIsActive(request.getIsActive() == null ? Boolean.TRUE : request.getIsActive());
    }

    private void validateClassCodeUnique(String rawCode, Long currentId) {
        String code = normalizeCode(rawCode);
        if (code == null) {
            return;
        }

        boolean existed = currentId == null
                ? classRepository.existsByClassCode(code)
                : classRepository.existsByClassCodeAndIdNot(code, currentId);
        if (existed) {
            throw new AppException(ErrorCode.VALUE_EXISTED, "Mã lớp đã tồn tại");
        }
    }

    private Clazzes findClassOrThrow(Long id) {
        return classRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Không tìm thấy lớp học"));
    }

    private Major findMajorOrThrow(Long majorId) {
        return majorRepository.findById(majorId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Không tìm thấy chuyên ngành"));
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
