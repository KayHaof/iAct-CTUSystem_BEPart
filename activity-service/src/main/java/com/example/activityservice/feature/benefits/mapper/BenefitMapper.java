package com.example.activityservice.feature.benefits.mapper;

import com.example.activityservice.feature.benefits.dto.BenefitResponse;
import com.example.activityservice.feature.benefits.model.Benefits;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BenefitMapper {

    @Mapping(source = "activity.id", target = "activityId")
    @Mapping(source = "category.id", target = "categoryId")
    @Mapping(source = "category.code", target = "categoryCode")
    @Mapping(source = "category.name", target = "categoryName")
    @Mapping(target = "typeLabel", expression = "java(toTypeLabel(benefit.getType()))")
    BenefitResponse toResponse(Benefits benefit);

    default String toTypeLabel(Integer type) {
        if (type == null) {
            return "Theo tiêu chí";
        }
        return switch (type) {
            case 1 -> "Điểm cộng";
            case 2 -> "Điểm trừ";
            default -> "Theo tiêu chí";
        };
    }

}
