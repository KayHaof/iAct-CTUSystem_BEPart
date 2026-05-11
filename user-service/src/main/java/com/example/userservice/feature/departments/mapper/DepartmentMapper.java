package com.example.userservice.feature.departments.mapper;

import com.example.userservice.feature.departments.dto.DepartmentRequest;
import com.example.userservice.feature.departments.dto.DepartmentResponse;
import com.example.userservice.feature.departments.model.Departments;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DepartmentMapper {
    DepartmentResponse toResponse(Departments entity);

    Departments toEntity(DepartmentRequest request);

    void updateEntityFromRequest(DepartmentRequest request, @MappingTarget Departments entity);
}