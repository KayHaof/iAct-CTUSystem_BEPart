package com.example.feature.major.mapper;

import com.example.feature.major.dto.MajorRequest;
import com.example.feature.major.dto.MajorResponse;
import com.example.feature.major.model.Major;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MajorMapper {
    @Mapping(source = "department.id", target = "departmentId")
    @Mapping(source = "department.name", target = "departmentName")
    MajorResponse toResponse(Major major);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "department", ignore = true)
    Major toEntity(MajorRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "department", ignore = true)
    void updateEntity(@MappingTarget Major major, MajorRequest request);
}