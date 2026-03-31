package com.example.feature.categories.service.Impl;

import com.example.exception.AppException;
import com.example.exception.ErrorCode;
import com.example.feature.categories.dto.CategoryRequest;
import com.example.feature.categories.dto.CategoryResponse;
import com.example.feature.categories.mapper.CategoryMapper;
import com.example.feature.categories.model.Categories;
import com.example.feature.categories.repository.CategoryRepository;
import com.example.feature.categories.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        log.info("Bắt đầu tạo danh mục mới với mã: {}", request.getCode());

        if (categoryRepository.existsByCode(request.getCode())) {
            throw new AppException(ErrorCode.INVALID_KEY, "Mã danh mục (Code) đã tồn tại!");
        }

        Categories category = new Categories();
        category.setCode(request.getCode());
        category.setName(request.getName());
        category.setMaxPoint(request.getMaxPoint());

        if (request.getParentId() != null) {
            Categories parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Không tìm thấy danh mục cha!"));
            category.setParent(parent);
        }

        Categories saved = categoryRepository.save(category);
        log.info("Tạo danh mục thành công: {}", saved.getId());
        return categoryMapper.toResponse(saved);
    }

    @Override
    public List<CategoryResponse> getAllCategoriesAsTree() {
        return categoryRepository.findByParentIsNull().stream()
                .map(categoryMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<CategoryResponse> getAllCategoriesFlat() {
        return categoryRepository.findAll().stream()
                .map(categoryMapper::toFlatResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CategoryResponse getCategoryById(Long id) {
        Categories category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Không tìm thấy danh mục!"));
        return categoryMapper.toResponse(category);
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        log.info("Đang cập nhật danh mục có ID: {}", id);

        Categories category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Không tìm thấy danh mục để cập nhật!"));

        if (categoryRepository.existsByCodeAndIdNot(request.getCode(), id)) {
            throw new AppException(ErrorCode.INVALID_KEY, "Mã danh mục (Code) đã được sử dụng bởi một danh mục khác!");
        }

        category.setCode(request.getCode());
        category.setName(request.getName());
        category.setMaxPoint(request.getMaxPoint());

        if (request.getParentId() != null) {
            if (request.getParentId().equals(id)) {
                throw new AppException(ErrorCode.INVALID_ACTION, "Danh mục không thể tự làm cha của chính nó!");
            }

            Categories parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Không tìm thấy danh mục cha được chỉ định!"));
            category.setParent(parent);
        } else {
            category.setParent(null);
        }

        Categories updated = categoryRepository.save(category);
        log.info("Cập nhật danh mục thành công: {}", updated.getId());
        return categoryMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        log.info("Đang xóa danh mục có ID: {}", id);

        Categories category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Không tìm thấy danh mục để xóa!"));

        categoryRepository.deleteById(id);
        log.info("Xóa danh mục thành công: {}", id);
    }
}