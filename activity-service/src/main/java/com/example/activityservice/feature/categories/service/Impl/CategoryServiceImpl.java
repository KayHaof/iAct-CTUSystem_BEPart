package com.example.activityservice.feature.categories.service.impl;

import com.example.activityservice.feature.activities.repository.ActivityRepository;
import com.example.activityservice.feature.award_criteria.repository.AwardCriteriaRepository;
import com.example.activityservice.feature.benefits.repository.BenefitRepository;
import com.example.activityservice.feature.categories.dto.CategoryRequest;
import com.example.activityservice.feature.categories.dto.CategoryResponse;
import com.example.activityservice.feature.categories.mapper.CategoryMapper;
import com.example.activityservice.feature.categories.model.Categories;
import com.example.activityservice.feature.categories.repository.CategoryRepository;
import com.example.activityservice.feature.categories.service.CategoryService;
import com.example.exception.AppException;
import com.example.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ActivityRepository activityRepository;
    private final BenefitRepository benefitRepository;
    private final AwardCriteriaRepository awardCriteriaRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        log.info("Creating category with code: {}", request.getCode());
        validateCodeUnique(request.getCode(), null);

        Categories category = new Categories();
        applyRequest(category, request);

        Categories saved = categoryRepository.save(category);
        return categoryMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategoriesAsTree(Boolean active) {
        List<Categories> categories = active == null
                ? categoryRepository.findAll()
                : categoryRepository.findByIsActive(active);
        return buildTree(categories);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategoriesFlat(Boolean active, Long parentId) {
        List<Categories> categories;
        if (active != null && parentId != null) {
            categories = categoryRepository.findByParentIdAndIsActive(parentId, active);
        } else if (active != null) {
            categories = categoryRepository.findByIsActive(active);
        } else if (parentId != null) {
            categories = categoryRepository.findByParentId(parentId);
        } else {
            categories = categoryRepository.findAll();
        }

        return categories.stream()
                .map(categoryMapper::toFlatResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        findCategoryOrThrow(id);
        CategoryResponse response = buildNodeMap(categoryRepository.findAll()).get(id);
        if (response == null) {
            throw new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Không tìm thấy danh mục!");
        }
        return response;
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        log.info("Updating category ID: {}", id);
        Categories category = findCategoryOrThrow(id);

        validateCodeUnique(request.getCode(), id);
        applyRequest(category, request);

        Categories updated = categoryRepository.save(category);
        return categoryMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public CategoryResponse activateCategory(Long id) {
        Categories category = findCategoryOrThrow(id);
        category.setIsActive(true);
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public CategoryResponse deactivateCategory(Long id) {
        Categories category = findCategoryOrThrow(id);
        category.setIsActive(false);
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        log.info("Deleting category ID: {}", id);
        Categories category = findCategoryOrThrow(id);

        if (hasBusinessReferences(id)) {
            category.setIsActive(false);
            categoryRepository.save(category);
            log.info("Category ID {} has references and was deactivated instead of deleted", id);
            return;
        }

        categoryRepository.delete(category);
    }

    private void applyRequest(Categories category, CategoryRequest request) {
        category.setCode(normalizeCode(request.getCode()));
        category.setName(request.getName().trim());
        category.setMaxPoint(request.getMaxPoint() == null ? 0 : request.getMaxPoint());
        category.setIsActive(request.getIsActive() == null ? Boolean.TRUE : request.getIsActive());

        if (request.getParentId() == null) {
            category.setParent(null);
            return;
        }

        if (category.getId() != null && request.getParentId().equals(category.getId())) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Danh mục không thể tự làm cha của chính nó!");
        }

        Categories parent = categoryRepository.findById(request.getParentId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Không tìm thấy danh mục cha!"));
        validateParentChain(category.getId(), parent);
        category.setParent(parent);
    }

    private void validateCodeUnique(String rawCode, Long currentId) {
        String code = normalizeCode(rawCode);
        if (code == null) {
            return;
        }

        boolean existed = currentId == null
                ? categoryRepository.existsByCode(code)
                : categoryRepository.existsByCodeAndIdNot(code, currentId);
        if (existed) {
            throw new AppException(ErrorCode.INVALID_KEY, "Mã danh mục đã tồn tại!");
        }
    }

    private void validateParentChain(Long categoryId, Categories parent) {
        if (categoryId == null) {
            return;
        }

        Set<Long> visitedIds = new HashSet<>();
        Categories current = parent;
        while (current != null) {
            if (!visitedIds.add(current.getId())) {
                throw new AppException(ErrorCode.INVALID_ACTION, "Cấu trúc danh mục đang có vòng lặp!");
            }
            if (categoryId.equals(current.getId())) {
                throw new AppException(ErrorCode.INVALID_ACTION, "Danh mục cha không được nằm trong danh mục con!");
            }
            current = current.getParent();
        }
    }

    private boolean hasBusinessReferences(Long categoryId) {
        return categoryRepository.existsByParentId(categoryId)
                || activityRepository.existsByCategoryId(categoryId)
                || benefitRepository.existsByCategoryId(categoryId)
                || awardCriteriaRepository.existsByCategoryId(categoryId);
    }

    private List<CategoryResponse> buildTree(List<Categories> categories) {
        Map<Long, CategoryResponse> nodes = buildNodeMap(categories);
        List<CategoryResponse> roots = new ArrayList<>();

        for (Categories category : categories) {
            CategoryResponse response = nodes.get(category.getId());
            Long parentId = getParentId(category);
            if (parentId != null && nodes.containsKey(parentId)) {
                nodes.get(parentId).getChildren().add(response);
            } else {
                roots.add(response);
            }
        }

        return roots;
    }

    private Map<Long, CategoryResponse> buildNodeMap(List<Categories> categories) {
        Map<Long, CategoryResponse> nodes = new LinkedHashMap<>();
        for (Categories category : categories) {
            CategoryResponse response = categoryMapper.toFlatResponse(category);
            response.setChildren(new ArrayList<>());
            nodes.put(category.getId(), response);
        }
        return nodes;
    }

    private Long getParentId(Categories category) {
        if (category.getParent() == null) {
            return null;
        }
        return category.getParent().getId();
    }

    private Categories findCategoryOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Không tìm thấy danh mục!"));
    }

    private String normalizeCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }
        return code.trim();
    }
}
