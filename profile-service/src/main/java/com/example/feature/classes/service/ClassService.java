package com.example.feature.classes.service;

import com.example.feature.classes.dto.ClassRequest;
import com.example.feature.classes.dto.ClassResponse;

import java.util.List;

public interface ClassService {
    List<Long> getClassIdsByDepartment(Long departmentId);
    List<ClassResponse> getClasses(Long majorId, Integer academicYear);
    ClassResponse createClass(ClassRequest request);
    ClassResponse updateClass(Long id, ClassRequest request);
    void deleteClass(Long id);
}