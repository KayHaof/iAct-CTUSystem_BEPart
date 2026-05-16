package com.example.activityservice.feature.semesters.service;

import com.example.activityservice.feature.semesters.dto.SemesterRequest;
import com.example.activityservice.feature.semesters.dto.SemesterResponse;

import java.util.List;

public interface SemesterService {
    SemesterResponse createSemester(SemesterRequest request);
    List<SemesterResponse> getAllSemesters(Boolean active, Boolean locked, String academicYear);
    SemesterResponse getSemesterById(Long id);
    SemesterResponse getActiveSemester();
    SemesterResponse updateSemester(Long id, SemesterRequest request);
    SemesterResponse activateSemester(Long id);
    SemesterResponse lockSemester(Long id);
    SemesterResponse unlockSemester(Long id);
    void deleteSemester(Long id);
}
