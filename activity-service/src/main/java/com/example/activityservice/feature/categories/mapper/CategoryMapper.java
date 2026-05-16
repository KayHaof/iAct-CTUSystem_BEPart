package com.example.activityservice.feature.categories.mapper;

import com.example.activityservice.feature.categories.dto.CategoryResponse;
import com.example.activityservice.feature.categories.model.Categories;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    @Mapping(source = "parent.id", target = "parentId")
    @Mapping(target = "children", ignore = true)
    CategoryResponse toResponse(Categories category);

    @Named("toFlat")
    @Mapping(source = "parent.id", target = "parentId")
    @Mapping(target = "children", ignore = true)
    CategoryResponse toFlatResponse(Categories category);
}
