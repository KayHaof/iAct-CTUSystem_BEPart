package com.example.userservice.feature.classes.service;

import com.example.dto.PageDTO;
import com.example.userservice.feature.classes.dto.ClassRequest;
import com.example.userservice.feature.classes.dto.ClassResponse;

import java.util.List;

public interface ClassService {
    List<Long> getClassIdsByDepartment(Long departmentId);

    List<ClassResponse> getClassesByMajor(Long majorId, String academicYear);

    PageDTO<ClassResponse> getClassPage(int page, int size, String keyword, Long departmentId, Long majorId, String academicYear, Boolean active);

    ClassResponse getClassById(Long id);

    ClassResponse createClass(ClassRequest request);

    ClassResponse updateClass(Long id, ClassRequest request);

    ClassResponse activateClass(Long id);

    ClassResponse deactivateClass(Long id);

    void deleteClass(Long id);
}
