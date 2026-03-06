package com.example.feature.semesters.service;

import com.example.feature.semesters.dto.SemesterRequest;
import com.example.feature.semesters.dto.SemesterResponse;
import java.util.List;

public interface SemesterService {
    SemesterResponse createSemester(SemesterRequest request);
    List<SemesterResponse> getAllSemesters();
    SemesterResponse getSemesterById(Long id);
    SemesterResponse getActiveSemester();
    SemesterResponse updateSemester(Long id, SemesterRequest request);
    void deleteSemester(Long id);
}