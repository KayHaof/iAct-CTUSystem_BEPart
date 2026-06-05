package com.example.userservice.feature.classes.mapper;

import com.example.userservice.feature.classes.dto.ClassRequest;
import com.example.userservice.feature.classes.dto.ClassResponse;
import com.example.userservice.feature.classes.model.Clazzes;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ClassMapper {

    @Mapping(source = "major.id", target = "majorId")
    @Mapping(source = "major.name", target = "majorName")
    @Mapping(source = "major.department.id", target = "departmentId")
    @Mapping(source = "major.department.name", target = "departmentName")
    ClassResponse toResponse(Clazzes clazz);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "major", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "academicYear", ignore = true)
    Clazzes toEntity(ClassRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "major", ignore = true)
    void updateEntity(@MappingTarget Clazzes clazz, ClassRequest request);
}
