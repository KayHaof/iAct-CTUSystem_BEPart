package com.example.feature.semesters.mapper;

import com.example.feature.semesters.dto.SemesterRequest;
import com.example.feature.semesters.dto.SemesterResponse;
import com.example.feature.semesters.model.Semesters;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface SemesterMapper {

    // Map ra Response (Dùng cho hàm GET)
    SemesterResponse toResponse(Semesters semester);

    // Map ngược từ Request vào Entity (Dùng cho hàm CREATE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Semesters toEntity(SemesterRequest request);

    // Đắp dữ liệu từ Request đè lên Entity có sẵn (Dùng cho hàm UPDATE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntityFromRequest(SemesterRequest request, @MappingTarget Semesters semester);
}
