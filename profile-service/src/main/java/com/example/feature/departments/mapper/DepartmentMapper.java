package com.example.feature.departments.mapper;

import com.example.feature.departments.dto.DepartmentRequest;
import com.example.feature.departments.dto.DepartmentResponse;
import com.example.feature.departments.model.Departments;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DepartmentMapper {
    DepartmentResponse toResponse(Departments entity);

    Departments toEntity(DepartmentRequest request);

    void updateEntityFromRequest(DepartmentRequest request, @MappingTarget Departments entity);
}