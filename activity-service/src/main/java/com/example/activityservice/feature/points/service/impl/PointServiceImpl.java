package com.example.activityservice.feature.points.service.impl;

import com.example.activityservice.feature.points.dto.*;
import com.example.activityservice.feature.benefits.repository.BenefitRepository;
import com.example.activityservice.feature.certificate_submissions.model.CertificateSubmission;
import com.example.activityservice.feature.certificate_submissions.repository.CertificateSubmissionRepository;
import com.example.activityservice.feature.points.service.PointService;
import com.example.activityservice.feature.points.service.PointCacheService;
import com.example.activityservice.feature.benefits.model.Benefits;
import com.example.activityservice.feature.categories.model.Categories;
import com.example.activityservice.feature.categories.repository.CategoryRepository;
import com.example.activityservice.feature.semesters.model.Semesters;
import com.example.activityservice.feature.semesters.repository.SemesterRepository;
import com.example.activityservice.feature.users.model.Users;
import com.example.activityservice.feature.users.repository.UserRepository;
import com.example.exception.AppException;
import com.example.exception.ErrorCode;
import com.example.util.UtcDateTime;
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
    private final CertificateSubmissionRepository certificateSubmissionRepository;
    private final PointCacheService pointCacheService;

    @Override
    @Transactional(readOnly = true)
    public PointSummaryResponse getStudentPointSummary(Long studentId, Long semesterId) {
        Users student = userRepository.findById(studentId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED, "Không tìm thấy sinh viên"));

        Semesters semester = resolveSemester(semesterId);
        if (semester == null) {
            semester = semesterRepository.findByIsActiveTrue()
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Không có học kỳ nào"));
        }

        Optional<PointSummaryResponse> cached = pointCacheService.getSummary(studentId, semester.getId());
        if (cached.isPresent()) {
            return cached.get();
        }

        List<Benefits> earnedBenefits = benefitRepository.findAwardedByStudentIdAndSemesterId(studentId, semester.getId());
        List<CertificateSubmission> approvedCertificates =
                certificateSubmissionRepository.findApprovedForPointSummary(studentId, semester.getId());

        int totalPoint = earnedBenefits.stream()
                .mapToInt(b -> b.getPoint() != null ? b.getPoint() : 0)
                .sum()
                + approvedCertificates.stream()
                .mapToInt(c -> c.getApprovedPoint() != null ? c.getApprovedPoint() : 0)
                .sum();

        int maxPoint = categoryRepository.sumMaxPointBySemesterId(semester.getId());
        double percentage = maxPoint > 0 ? (totalPoint * 100.0) / maxPoint : 0;

        List<CategoryPointItem> breakdown = buildCategoryBreakdown(earnedBenefits, approvedCertificates);
        List<String> warnings = generateWarnings(totalPoint, maxPoint, breakdown);

        String status = calculateStatus(percentage);

        PointSummaryResponse response = PointSummaryResponse.builder()
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
        pointCacheService.putSummary(studentId, semester.getId(), response);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public PointDetailsResponse getStudentPointDetails(Long studentId, Long semesterId) {
        userRepository.findById(studentId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Semesters semester = resolveSemester(semesterId);

        Optional<PointDetailsResponse> cached = pointCacheService.getDetails(studentId, semester.getId());
        if (cached.isPresent()) {
            return cached.get();
        }

        List<Benefits> earnedBenefits = benefitRepository.findAwardedByStudentIdAndSemesterId(studentId, semester.getId());
        List<CertificateSubmission> approvedCertificates =
                certificateSubmissionRepository.findApprovedForPointSummary(studentId, semester.getId());

        int totalPoint = earnedBenefits.stream()
                .mapToInt(b -> b.getPoint() != null ? b.getPoint() : 0)
                .sum()
                + approvedCertificates.stream()
                .mapToInt(c -> c.getApprovedPoint() != null ? c.getApprovedPoint() : 0)
                .sum();

        int maxPoint = categoryRepository.sumMaxPointBySemesterId(semester.getId());

        List<CategoryDetail> categories = buildCategoryDetails(earnedBenefits, approvedCertificates);

        PointDetailsResponse response = PointDetailsResponse.builder()
                .studentId(studentId)
                .semesterId(semester.getId())
                .totalPoint(totalPoint)
                .maxPoint(maxPoint)
                .categories(categories)
                .details(buildContributionDetails(earnedBenefits, approvedCertificates))
                .build();
        pointCacheService.putDetails(studentId, semester.getId(), response);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryPointResponse> getCategoriesWithPoints(Long semesterId) {
        Semesters semester = resolveSemester(semesterId);

        Optional<List<CategoryPointResponse>> cached = pointCacheService.getCategories(semester.getId());
        if (cached.isPresent()) {
            return cached.get();
        }

        List<Categories> rootCategories = categoryRepository.findRootCategories(semester.getId());

        List<CategoryPointResponse> response = rootCategories.stream()
                .map(this::buildCategoryTree)
                .collect(Collectors.toList());
        pointCacheService.putCategories(semester.getId(), response);
        return response;
    }

    @Override
    public Long getStudentIdByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(user -> user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED, 
                        "Không tìm thấy sinh viên với username: " + username));
    }

    private Semesters resolveSemester(Long semesterId) {
        if (semesterId != null) {
            return semesterRepository.findById(semesterId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Không tìm thấy học kỳ"));
        }
        return semesterRepository.findByIsActiveTrue()
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Không có học kỳ hiện tại"));
    }

    private List<CategoryPointItem> buildCategoryBreakdown(
            List<Benefits> benefits,
            List<CertificateSubmission> certificates) {
        Map<Long, CategoryAccumulator> byCategory = categoryAccumulators(benefits, certificates);

        return byCategory.values().stream()
                .map(accumulator -> CategoryPointItem.builder()
                        .categoryId(accumulator.category().getId())
                        .categoryCode(accumulator.category().getCode())
                        .categoryName(accumulator.category().getName())
                        .earnedPoint(accumulator.earnedPoint())
                        .maxPoint(accumulator.category().getMaxPoint())
                        .percentage(percentage(accumulator.earnedPoint(), accumulator.category().getMaxPoint()))
                        .build())
                .collect(Collectors.toList());
    }

    private List<CategoryDetail> buildCategoryDetails(
            List<Benefits> benefits,
            List<CertificateSubmission> certificates) {
        Map<Long, CategoryAccumulator> byCategory = categoryAccumulators(benefits, certificates);

        return byCategory.values().stream()
                .map(accumulator -> CategoryDetail.builder()
                        .id(accumulator.category().getId())
                        .code(accumulator.category().getCode())
                        .name(accumulator.category().getName())
                        .maxPoint(accumulator.category().getMaxPoint())
                        .earnedPoint(accumulator.earnedPoint())
                        .percentage(percentage(accumulator.earnedPoint(), accumulator.category().getMaxPoint()))
                        .criteria(Collections.emptyList())
                        .build())
                .collect(Collectors.toList());
    }

    private Map<Long, CategoryAccumulator> categoryAccumulators(
            List<Benefits> benefits,
            List<CertificateSubmission> certificates) {
        Map<Long, CategoryAccumulator> byCategory = new LinkedHashMap<>();
        benefits.stream()
                .filter(benefit -> benefit.getCategory() != null)
                .forEach(benefit -> addCategoryPoint(
                        byCategory,
                        benefit.getCategory(),
                        benefit.getPoint() != null ? benefit.getPoint() : 0));
        certificates.stream()
                .filter(certificate -> certificate.getApprovedCategory() != null)
                .forEach(certificate -> addCategoryPoint(
                        byCategory,
                        certificate.getApprovedCategory(),
                        certificate.getApprovedPoint() != null ? certificate.getApprovedPoint() : 0));
        return byCategory;
    }

    private void addCategoryPoint(Map<Long, CategoryAccumulator> byCategory, Categories category, int point) {
        CategoryAccumulator current = byCategory.get(category.getId());
        byCategory.put(category.getId(), new CategoryAccumulator(
                category,
                (current != null ? current.earnedPoint() : 0) + point));
    }

    private List<PointContributionDetail> buildContributionDetails(
            List<Benefits> benefits,
            List<CertificateSubmission> certificates) {
        List<PointContributionDetail> details = new ArrayList<>();
        benefits.stream()
                .map(benefit -> {
                    var activity = benefit.getActivity();
                    var category = benefit.getCategory();
                    return PointContributionDetail.builder()
                            .sourceType("ACTIVITY")
                            .activityId(activity != null ? activity.getId() : null)
                            .activityTitle(activity != null ? activity.getTitle() : null)
                            .categoryId(category != null ? category.getId() : null)
                            .categoryName(category != null ? category.getName() : null)
                            .earnedPoint(benefit.getPoint() != null ? benefit.getPoint() : 0)
                            .attendedAt(activity != null ? UtcDateTime.format(activity.getEndDate()) : null)
                            .proofStatus(1)
                            .build();
                })
                .forEach(details::add);
        certificates.stream()
                .map(certificate -> PointContributionDetail.builder()
                        .sourceType("CERTIFICATE_SUBMISSION")
                        .certificateSubmissionId(certificate.getId())
                        .certificateTitle(certificate.getCertificateTitle())
                        .categoryId(certificate.getApprovedCategory() != null
                                ? certificate.getApprovedCategory().getId()
                                : null)
                        .categoryName(certificate.getApprovedCategory() != null
                                ? certificate.getApprovedCategory().getName()
                                : null)
                        .earnedPoint(certificate.getApprovedPoint() != null ? certificate.getApprovedPoint() : 0)
                        .attendedAt(UtcDateTime.format(certificate.getReviewedAt()))
                        .proofStatus(1)
                        .build())
                .forEach(details::add);
        return details;
    }

    private Double percentage(int earnedPoint, Integer maxPoint) {
        return maxPoint != null && maxPoint > 0
                ? Math.round((earnedPoint * 100.0) / maxPoint * 10.0) / 10.0
                : 0;
    }

    private record CategoryAccumulator(Categories category, int earnedPoint) {}

    private CategoryPointResponse buildCategoryTree(Categories category) {
        List<CategoryPointResponse> children = category.getSubCategories() != null
                ? category.getSubCategories().stream()
                    .filter(child -> child != null && Boolean.TRUE.equals(child.getIsActive()))
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
            warnings.add("Bạn đang thiếu " + (maxPoint - totalPoint) + " điểm để đạt mức tốt (>50%)");
        }

        for (CategoryPointItem item : breakdown) {
            if (item.getMaxPoint() > 0) {
                double catPercentage = (item.getEarnedPoint() * 100.0) / item.getMaxPoint();
                if (catPercentage < 30) {
                    warnings.add("Bạn đang thiếu " + (item.getMaxPoint() - item.getEarnedPoint())
                            + " điểm ở " + item.getCategoryCode() + " - " + item.getCategoryName());
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
