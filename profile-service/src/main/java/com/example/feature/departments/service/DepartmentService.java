package com.example.feature.departments.service;

import com.example.dto.PageDTO;
import com.example.feature.departments.dto.DepartmentRequest;
import com.example.feature.departments.dto.DepartmentResponse;
import java.util.List;

public interface DepartmentService {
    DepartmentResponse createDepartment(DepartmentRequest request);
    DepartmentResponse updateDepartment(Long id, DepartmentRequest request);
    void deleteDepartment(Long id);
    DepartmentResponse getDepartmentById(Long id);
    PageDTO<DepartmentResponse> getDepartments(int page, int size, String keyword);
    List<DepartmentResponse> getAllDepartments();
}