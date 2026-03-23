package com.example.feature.departments.service.impl;

import com.example.dto.PageDTO;
import com.example.exception.AppException;
import com.example.exception.ErrorCode;
import com.example.feature.departments.dto.DepartmentRequest;
import com.example.feature.departments.dto.DepartmentResponse;
import com.example.feature.departments.mapper.DepartmentMapper;
import com.example.feature.departments.model.Departments;
import com.example.feature.departments.repository.DepartmentRepository;
import com.example.feature.departments.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final DepartmentMapper departmentMapper;

    @Override
    @Transactional
    public DepartmentResponse createDepartment(DepartmentRequest request) {
        Departments entity = departmentMapper.toEntity(request);
        Departments savedEntity = departmentRepository.save(entity);
        return departmentMapper.toResponse(savedEntity);
    }

    @Override
    @Transactional
    public DepartmentResponse updateDepartment(Long id, DepartmentRequest request) {
        Departments existingDepartment = departmentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Department not found with id: " + id));

        departmentMapper.updateEntityFromRequest(request, existingDepartment);

        Departments updatedEntity = departmentRepository.save(existingDepartment);
        return departmentMapper.toResponse(updatedEntity);
    }

    @Override
    @Transactional
    public void deleteDepartment(Long id) {
        if (!departmentRepository.existsById(id)) {
            throw new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Department not found with id: " + id);
        }
        departmentRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentResponse getDepartmentById(Long id) {
        Departments entity = departmentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Department not found with id: " + id));
        return departmentMapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public PageDTO<DepartmentResponse> getDepartments(int page, int size, String keyword) {
        Pageable pageable = PageRequest.of(page > 0 ? page - 1 : 0, size, Sort.by("id").descending());

        Specification<Departments> spec = (root, query, cb) -> {
            if (keyword == null || keyword.trim().isEmpty()) {
                return cb.conjunction();
            }
            String likeKeyword = "%" + keyword.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("name")), likeKeyword),
                    cb.like(cb.lower(root.get("description")), likeKeyword)
            );
        };

        Page<Departments> departmentPage = departmentRepository.findAll(spec, pageable);

        List<DepartmentResponse> data = departmentPage.getContent().stream()
                .map(departmentMapper::toResponse)
                .collect(Collectors.toList());

        return new PageDTO<>(departmentPage, data);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentResponse> getAllDepartments() {
        return departmentRepository.findAll().stream()
                .map(departmentMapper::toResponse)
                .collect(Collectors.toList());
    }
}