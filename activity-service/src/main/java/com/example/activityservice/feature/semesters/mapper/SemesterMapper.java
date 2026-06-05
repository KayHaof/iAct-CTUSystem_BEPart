package com.example.activityservice.feature.semesters.mapper;

import com.example.activityservice.feature.semesters.dto.SemesterRequest;
import com.example.activityservice.feature.semesters.dto.SemesterResponse;
import com.example.activityservice.feature.semesters.model.Semesters;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SemesterMapper {

    @Mapping(target = "semesterName", source = "name")
    SemesterResponse toResponse(Semesters semester);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "activities", ignore = true)
    @Mapping(target = "complaints", ignore = true)
    @Mapping(target = "studentAwards", ignore = true)
    @Mapping(target = "name", expression = "java(resolveName(request))")
    Semesters toEntity(SemesterRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "activities", ignore = true)
    @Mapping(target = "complaints", ignore = true)
    @Mapping(target = "studentAwards", ignore = true)
    @Mapping(target = "name", expression = "java(resolveName(request))")
    void updateEntityFromRequest(SemesterRequest request, @MappingTarget Semesters semester);

    default String resolveName(SemesterRequest request) {
        if (request == null) {
            return null;
        }
        return request.getName() != null ? request.getName() : request.getSemesterName();
    }
}
