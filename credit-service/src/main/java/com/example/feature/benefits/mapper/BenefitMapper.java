package com.example.feature.benefits.mapper;

import com.example.feature.benefits.dto.BenefitResponse;
import com.example.feature.benefits.model.Benefits;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BenefitMapper {

    @Mapping(source = "activity.id", target = "activityId")
    @Mapping(source = "category.id", target = "categoryId")
    @Mapping(source = "category.name", target = "categoryName")
    BenefitResponse toResponse(Benefits benefit);

}
