package com.example.feature.departments.mapper;

import com.example.feature.departments.dto.DepartmentRequest;
import com.example.feature.departments.dto.DepartmentResponse;
import com.example.feature.departments.model.Departments;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface DepartmentMapper {
    // 1. Chuyển từ Entity sang Response DTO
    DepartmentResponse toResponse(Departments entity);

    // 2. Chuyển từ Request DTO sang Entity (Dùng cho Create)
    Departments toEntity(DepartmentRequest request);

    // 3. Cập nhật Entity có sẵn từ Request DTO (Dùng cho Update)
    void updateEntityFromRequest(DepartmentRequest request, @MappingTarget Departments entity);
}