package com.example.userservice.feature.major.service;

import com.example.dto.PageDTO;
import com.example.userservice.feature.major.dto.MajorRequest;
import com.example.userservice.feature.major.dto.MajorResponse;

import java.util.List;

public interface MajorService {
    List<MajorResponse> getMajors(Long departmentId, Boolean active);

    PageDTO<MajorResponse> getMajorPage(
            int page,
            int size,
            String keyword,
            Long departmentId,
            Boolean active,
            String programType);

    MajorResponse getMajorById(Long id);

    MajorResponse createMajor(MajorRequest request);

    MajorResponse updateMajor(Long id, MajorRequest request);

    MajorResponse activateMajor(Long id);

    MajorResponse deactivateMajor(Long id);

    void deleteMajor(Long id);
}
