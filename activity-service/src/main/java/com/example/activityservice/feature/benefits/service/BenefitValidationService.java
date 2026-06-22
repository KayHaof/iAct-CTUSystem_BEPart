package com.example.activityservice.feature.benefits.service;

import com.example.activityservice.feature.categories.model.Categories;
import com.example.activityservice.feature.categories.repository.CategoryRepository;
import com.example.exception.AppException;
import com.example.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BenefitValidationService {

    private final CategoryRepository categoryRepository;

    public Categories validateAndGetCategory(Long categoryId, Integer point, Integer type) {
        if (categoryId == null) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Vui lòng chọn tiêu chí điểm rèn luyện!");
        }
        if (point == null || point < 0) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Điểm quyền lợi phải lớn hơn hoặc bằng 0!");
        }
        if (type == null || type < 1 || type > 3) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Loại quyền lợi không hợp lệ!");
        }

        Categories category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new AppException(
                        ErrorCode.RESOURCE_NOT_EXISTED,
                        "Không tìm thấy tiêu chí điểm rèn luyện!"));

        if (!Boolean.TRUE.equals(category.getIsActive())) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Tiêu chí điểm rèn luyện đã ngừng sử dụng!");
        }
        if (categoryRepository.existsByParentIdAndIsActive(categoryId, true)) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Chỉ được chọn tiêu chí nhỏ nhất để cộng điểm!");
        }

        int maxPoint = category.getMaxPoint() == null ? 0 : category.getMaxPoint();
        if (point > maxPoint) {
            throw new AppException(
                    ErrorCode.INVALID_ACTION,
                    "Điểm quyền lợi không được vượt quá " + maxPoint + " điểm của tiêu chí đã chọn!");
        }

        return category;
    }
}
