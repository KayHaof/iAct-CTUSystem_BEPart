package com.example.feature.classes.service.Impl;

import com.example.exception.AppException;
import com.example.exception.ErrorCode;
import com.example.feature.classes.dto.ClassRequest;
import com.example.feature.classes.dto.ClassResponse;
import com.example.feature.classes.mapper.ClassMapper;
import com.example.feature.classes.model.Clazzes;
import com.example.feature.classes.repository.ClassRepository;
import com.example.feature.classes.service.ClassService;
import com.example.feature.major.model.Major;
import com.example.feature.major.repository.MajorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
    public List<ClassResponse> getClasses(Long majorId, Integer academicYear) {
        List<Clazzes> classes = (majorId != null)
                ? classRepository.findClassesByMajorAndYear(majorId, academicYear)
                : classRepository.findAll();
        return classes.stream().map(classMapper::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ClassResponse createClass(ClassRequest request) {
        if (classRepository.existsByClassCode(request.getClassCode())) {
            throw new AppException(ErrorCode.VALUE_EXISTED, "Mã lớp đã tồn tại");
        }

        Major major = majorRepository.findById(request.getMajorId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Không tìm thấy Chuyên ngành"));

        Clazzes clazz = classMapper.toEntity(request);
        clazz.setMajor(major);

        return classMapper.toResponse(classRepository.save(clazz));
    }

    @Override
    @Transactional
    public ClassResponse updateClass(Long id, ClassRequest request) {
        Clazzes clazz = classRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Không tìm thấy Lớp học"));

        if (!clazz.getClassCode().equals(request.getClassCode()) &&
                classRepository.existsByClassCode(request.getClassCode())) {
            throw new AppException(ErrorCode.VALUE_EXISTED, "Mã lớp đã tồn tại");
        }

        Major major = majorRepository.findById(request.getMajorId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Không tìm thấy Chuyên ngành"));

        classMapper.updateEntity(clazz, request);
        clazz.setMajor(major);

        return classMapper.toResponse(classRepository.save(clazz));
    }

    @Override
    @Transactional
    public void deleteClass(Long id) {
        if (!classRepository.existsById(id)) {
            throw new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Không tìm thấy Lớp học");
        }
        classRepository.deleteById(id);
    }
}
