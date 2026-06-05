package com.example.activityservice.feature.points.service.impl;

import com.example.activityservice.feature.points.dto.*;
import com.example.activityservice.feature.benefits.repository.BenefitRepository;
import com.example.activityservice.feature.points.service.PointService;
import com.example.activityservice.feature.benefits.model.Benefits;
import com.example.activityservice.feature.categories.model.Categories;
import com.example.activityservice.feature.categories.repository.CategoryRepository;
import com.example.activityservice.feature.semesters.model.Semesters;
import com.example.activityservice.feature.semesters.repository.SemesterRepository;
import com.example.activityservice.feature.users.model.Users;
import com.example.activityservice.feature.users.repository.UserRepository;
import com.example.exception.AppException;
import com.example.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PointServiceImpl implements PointService {

    private final UserRepository userRepository;
    private final SemesterRepository semesterRepository;
    private final CategoryRepository categoryRepository;
    private final BenefitRepository benefitRepository;

    @Override
    @Transactional(readOnly = true)
    public PointSummaryResponse getStudentPointSummary(Long studentId, Long semesterId) {
        Users student = userRepository.findById(studentId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED, "Khong tim thay sinh vien"));

        Semesters semester = resolveSemester(semesterId);
        if (semester == null) {
            semester = semesterRepository.findByIsActiveTrue()
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Khong co hoc ky nao"));
        }

        List<Benefits> earnedBenefits = benefitRepository.findByStudentIdAndSemesterId(studentId, semester.getId());

        int totalPoint = earnedBenefits.stream()
                .mapToInt(b -> b.getPoint() != null ? b.getPoint() : 0)
                .sum();

        int maxPoint = categoryRepository.sumMaxPointBySemesterId(semester.getId());
        double percentage = maxPoint > 0 ? (totalPoint * 100.0) / maxPoint : 0;

        List<CategoryPointItem> breakdown = buildCategoryBreakdown(earnedBenefits);
        List<String> warnings = generateWarnings(totalPoint, maxPoint, breakdown);

        String status = calculateStatus(percentage);

        return PointSummaryResponse.builder()
                .studentId(studentId)
                .studentCode(student.getStudentCode())
                .studentName(student.getFullName())
                .semesterId(semester.getId())
                .semesterName(semester.getName())
                .totalPoint(totalPoint)
                .maxPoint(maxPoint)
                .percentage(Math.round(percentage * 10.0) / 10.0)
                .status(status)
                .categoryBreakdown(breakdown)
                .warnings(warnings)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PointDetailsResponse getStudentPointDetails(Long studentId, Long semesterId) {
        userRepository.findById(studentId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Semesters semester = resolveSemester(semesterId);

        List<Benefits> earnedBenefits = benefitRepository.findByStudentIdAndSemesterId(studentId, semester.getId());

        int totalPoint = earnedBenefits.stream()
                .mapToInt(b -> b.getPoint() != null ? b.getPoint() : 0)
                .sum();

        int maxPoint = categoryRepository.sumMaxPointBySemesterId(semester.getId());

        List<CategoryDetail> categories = buildCategoryDetails(earnedBenefits);

        return PointDetailsResponse.builder()
                .studentId(studentId)
                .semesterId(semester.getId())
                .totalPoint(totalPoint)
                .maxPoint(maxPoint)
                .categories(categories)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryPointResponse> getCategoriesWithPoints(Long semesterId) {
        Semesters semester = resolveSemester(semesterId);

        List<Categories> rootCategories = categoryRepository.findRootCategories(semester.getId());

        return rootCategories.stream()
                .map(this::buildCategoryTree)
                .collect(Collectors.toList());
    }

    @Override
    public Long getStudentIdByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(Users::getId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED, 
                        "Khong tim thay sinh vien voi username: " + username));
    }

    private Semesters resolveSemester(Long semesterId) {
        if (semesterId != null) {
            return semesterRepository.findById(semesterId)
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Khong tim thay hoc ky"));
        }
        return semesterRepository.findByIsActiveTrue()
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Khong co hoc ky hien tai"));
    }

    private List<CategoryPointItem> buildCategoryBreakdown(List<Benefits> benefits) {
        Map<Long, List<Benefits>> byCategory = benefits.stream()
                .filter(b -> b.getCategory() != null)
                .collect(Collectors.groupingBy(b -> b.getCategory().getId()));

        List<CategoryPointItem> breakdown = new ArrayList<>();

        for (Map.Entry<Long, List<Benefits>> entry : byCategory.entrySet()) {
            Categories category = entry.getValue().get(0).getCategory();
            int earned = entry.getValue().stream()
                    .mapToInt(b -> b.getPoint() != null ? b.getPoint() : 0)
                    .sum();

            breakdown.add(CategoryPointItem.builder()
                    .categoryId(category.getId())
                    .categoryCode(category.getCode())
                    .categoryName(category.getName())
                    .earnedPoint(earned)
                    .maxPoint(category.getMaxPoint())
                    .percentage(category.getMaxPoint() > 0 ? Math.round((earned * 100.0) / category.getMaxPoint() * 10.0) / 10.0 : 0)
                    .build());
        }

        return breakdown;
    }

    private List<CategoryDetail> buildCategoryDetails(List<Benefits> benefits) {
        Map<Long, List<Benefits>> byCategory = benefits.stream()
                .filter(b -> b.getCategory() != null)
                .collect(Collectors.groupingBy(b -> b.getCategory().getId()));

        List<CategoryDetail> categories = new ArrayList<>();

        for (Map.Entry<Long, List<Benefits>> entry : byCategory.entrySet()) {
            Categories category = entry.getValue().get(0).getCategory();
            int earned = entry.getValue().stream()
                    .mapToInt(b -> b.getPoint() != null ? b.getPoint() : 0)
                    .sum();

            categories.add(CategoryDetail.builder()
                    .id(category.getId())
                    .code(category.getCode())
                    .name(category.getName())
                    .maxPoint(category.getMaxPoint())
                    .earnedPoint(earned)
                    .percentage(category.getMaxPoint() > 0 ? Math.round((earned * 100.0) / category.getMaxPoint() * 10.0) / 10.0 : 0)
                    .criteria(Collections.emptyList())
                    .build());
        }

        return categories;
    }

    private CategoryPointResponse buildCategoryTree(Categories category) {
        List<CategoryPointResponse> children = category.getSubCategories() != null
                ? category.getSubCategories().stream()
                    .filter(Categories::getIsActive)
                    .map(this::buildCategoryTree)
                    .collect(Collectors.toList())
                : Collections.emptyList();

        return CategoryPointResponse.builder()
                .id(category.getId())
                .code(category.getCode())
                .name(category.getName())
                .maxPoint(category.getMaxPoint())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .children(children.isEmpty() ? null : children)
                .build();
    }

    private List<String> generateWarnings(int totalPoint, int maxPoint, List<CategoryPointItem> breakdown) {
        List<String> warnings = new ArrayList<>();

        if (maxPoint == 0) return warnings;

        double overallPercentage = (totalPoint * 100.0) / maxPoint;
        if (overallPercentage < 50) {
            warnings.add("Ban dang thieu " + (maxPoint - totalPoint) + " diem de dat muc tot (>50%)");
        }

        for (CategoryPointItem item : breakdown) {
            if (item.getMaxPoint() > 0) {
                double catPercentage = (item.getEarnedPoint() * 100.0) / item.getMaxPoint();
                if (catPercentage < 30) {
                    warnings.add("Ban dang thieu " + (item.getMaxPoint() - item.getEarnedPoint()) 
                            + " diem o " + item.getCategoryCode() + " - " + item.getCategoryName());
                }
            }
        }

        return warnings;
    }

    private String calculateStatus(double percentage) {
        if (percentage >= 80) return "excellent";
        if (percentage >= 50) return "good";
        if (percentage >= 30) return "warning";
        return "danger";
    }
}
