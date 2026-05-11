package com.example.userservice.feature.major.service;

import com.example.userservice.feature.major.dto.MajorRequest;
import com.example.userservice.feature.major.dto.MajorResponse;

import java.util.List;

public interface MajorService {
    List<MajorResponse> getMajors(Long departmentId);
    MajorResponse createMajor(MajorRequest request);
    MajorResponse updateMajor(Long id, MajorRequest request);
    void deleteMajor(Long id);
}
