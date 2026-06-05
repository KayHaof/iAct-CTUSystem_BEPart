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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
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
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String CACHE_KEY_ALL_TREE = "categories:tree";
    private static final String CACHE_KEY_FLAT_PREFIX = "categories:flat:";
    private static final long CACHE_TTL_MINUTES = 10;

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        log.info("Creating category with code: {}", request.getCode());
        validateCodeUnique(request.getCode(), null);

        Categories category = new Categories();
        applyRequest(category, request);

        Categories saved = categoryRepository.save(category);
        evictAllCategoryCaches();
        return categoryMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategoriesAsTree(Boolean active) {
        String cacheKey = active == null ? CACHE_KEY_ALL_TREE : CACHE_KEY_ALL_TREE + ":" + active;

        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                log.debug("Cache hit for categories tree (active={})", active);
                return objectMapper.convertValue(cached, new TypeReference<List<CategoryResponse>>() {});
            }
        } catch (Exception e) {
            log.warn("Redis read failed for categories tree, falling back to DB: {}", e.getMessage());
        }

        List<Categories> categories = active == null
                ? categoryRepository.findAll()
                : categoryRepository.findByIsActive(active);
        List<CategoryResponse> result = buildTree(categories);

        try {
            redisTemplate.opsForValue().set(cacheKey, result, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Redis write failed for categories tree: {}", e.getMessage());
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategoriesFlat(Boolean active, Long parentId) {
        String cacheKey = CACHE_KEY_FLAT_PREFIX
                + (active == null ? "all" : active)
                + ":" + (parentId == null ? "root" : parentId);

        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                log.debug("Cache hit for categories flat (active={}, parentId={})", active, parentId);
                return objectMapper.convertValue(cached, new TypeReference<List<CategoryResponse>>() {});
            }
        } catch (Exception e) {
            log.warn("Redis read failed for categories flat, falling back to DB: {}", e.getMessage());
        }

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

        List<CategoryResponse> result = categories.stream()
                .map(categoryMapper::toFlatResponse)
                .collect(Collectors.toList());

        try {
            redisTemplate.opsForValue().set(cacheKey, result, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Redis write failed for categories flat: {}", e.getMessage());
        }

        return result;
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
        evictAllCategoryCaches();
        return categoryMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public CategoryResponse activateCategory(Long id) {
        Categories category = findCategoryOrThrow(id);
        category.setIsActive(true);
        CategoryResponse response = categoryMapper.toResponse(categoryRepository.save(category));
        evictAllCategoryCaches();
        return response;
    }

    @Override
    @Transactional
    public CategoryResponse deactivateCategory(Long id) {
        Categories category = findCategoryOrThrow(id);
        category.setIsActive(false);
        CategoryResponse response = categoryMapper.toResponse(categoryRepository.save(category));
        evictAllCategoryCaches();
        return response;
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
            evictAllCategoryCaches();
            return;
        }

        categoryRepository.delete(category);
        evictAllCategoryCaches();
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

    private void evictAllCategoryCaches() {
        try {
            Set<String> keys = redisTemplate.keys(CACHE_KEY_FLAT_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
            redisTemplate.delete(CACHE_KEY_ALL_TREE);
            redisTemplate.delete(CACHE_KEY_ALL_TREE + ":true");
            redisTemplate.delete(CACHE_KEY_ALL_TREE + ":false");
            log.info("Category caches evicted");
        } catch (Exception e) {
            log.warn("Failed to evict category caches: {}", e.getMessage());
        }
    }
}
