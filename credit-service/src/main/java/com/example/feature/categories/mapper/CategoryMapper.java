package com.example.feature.categories.mapper;

import com.example.feature.categories.dto.CategoryResponse;
import com.example.feature.categories.model.Categories;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    // Map dạng cây: Tự động map thuộc tính parent.id vào parentId và đệ quy list children
    @Mapping(source = "parent.id", target = "parentId")
    CategoryResponse toResponse(Categories category);

    // Map dạng phẳng: Giống ở trên nhưng bỏ qua việc map mảng children
    @Mapping(source = "parent.id", target = "parentId")
    @Mapping(target = "children", ignore = true)
    CategoryResponse toFlatResponse(Categories category);
}