package com.example.activityservice.feature.benefits.service;

import com.example.activityservice.feature.categories.model.Categories;
import com.example.activityservice.feature.categories.repository.CategoryRepository;
import com.example.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BenefitValidationServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private BenefitValidationService validationService;

    private Categories leafCategory;

    @BeforeEach
    void setUp() {
        leafCategory = Categories.builder()
                .id(10L)
                .name("Tiêu chí lá")
                .maxPoint(5)
                .isActive(true)
                .build();
    }

    @Test
    void acceptsPointWithinLeafCategoryLimit() {
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(leafCategory));
        when(categoryRepository.existsByParentIdAndIsActive(10L, true)).thenReturn(false);

        Categories result = validationService.validateAndGetCategory(10L, 5, 1);

        assertSame(leafCategory, result);
    }

    @Test
    void rejectsNegativePoint() {
        assertThrows(AppException.class, () -> validationService.validateAndGetCategory(10L, -1, 1));
    }

    @Test
    void rejectsPointAboveCategoryLimit() {
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(leafCategory));
        when(categoryRepository.existsByParentIdAndIsActive(10L, true)).thenReturn(false);

        assertThrows(AppException.class, () -> validationService.validateAndGetCategory(10L, 6, 1));
    }

    @Test
    void rejectsParentCategory() {
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(leafCategory));
        when(categoryRepository.existsByParentIdAndIsActive(10L, true)).thenReturn(true);

        assertThrows(AppException.class, () -> validationService.validateAndGetCategory(10L, 3, 1));
    }
}
