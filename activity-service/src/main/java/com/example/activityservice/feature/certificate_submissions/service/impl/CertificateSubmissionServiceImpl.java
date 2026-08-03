package com.example.activityservice.feature.certificate_submissions.service.impl;

import com.example.activityservice.feature.categories.model.Categories;
import com.example.activityservice.feature.categories.repository.CategoryRepository;
import com.example.activityservice.feature.certificate_submissions.ai.CertificateAiClient;
import com.example.activityservice.feature.certificate_submissions.ai.CertificateAiScanResult;
import com.example.activityservice.feature.certificate_submissions.ai.CertificateImageFetcher;
import com.example.activityservice.feature.certificate_submissions.ai.FetchedCertificateImage;
import com.example.activityservice.feature.certificate_submissions.dto.CertificateSubmissionRejectRequest;
import com.example.activityservice.feature.certificate_submissions.dto.CertificateSubmissionRequest;
import com.example.activityservice.feature.certificate_submissions.dto.CertificateSubmissionResponse;
import com.example.activityservice.feature.certificate_submissions.dto.CertificateSubmissionReviewRequest;
import com.example.activityservice.feature.certificate_submissions.kafka.CertificateSubmissionEventProducer;
import com.example.activityservice.feature.certificate_submissions.mapper.CertificateSubmissionMapper;
import com.example.activityservice.feature.certificate_submissions.model.CertificateSubmission;
import com.example.activityservice.feature.certificate_submissions.repository.CertificateSubmissionRepository;
import com.example.activityservice.feature.certificate_submissions.service.CertificateCategorySuggestion;
import com.example.activityservice.feature.certificate_submissions.service.CertificateCategorySuggestionService;
import com.example.activityservice.feature.points.kafka.PointEventProducer;
import com.example.activityservice.feature.points.service.PointCacheService;
import com.example.activityservice.feature.semesters.model.Semesters;
import com.example.activityservice.feature.semesters.repository.SemesterRepository;
import com.example.activityservice.feature.users.model.Users;
import com.example.activityservice.feature.users.repository.UserRepository;
import com.example.dto.PageDTO;
import com.example.exception.AppException;
import com.example.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CertificateSubmissionServiceImpl implements com.example.activityservice.feature.certificate_submissions.service.CertificateSubmissionService {

    private final CertificateSubmissionRepository certificateSubmissionRepository;
    private final UserRepository userRepository;
    private final SemesterRepository semesterRepository;
    private final CategoryRepository categoryRepository;
    private final CertificateImageFetcher imageFetcher;
    private final CertificateAiClient certificateAiClient;
    private final CertificateCategorySuggestionService categorySuggestionService;
    private final CertificateSubmissionMapper mapper;
    private final CertificateSubmissionEventProducer eventProducer;
    private final PointEventProducer pointEventProducer;
    private final PointCacheService pointCacheService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public CertificateSubmissionResponse submit(CertificateSubmissionRequest request) {
        Users student = getCurrentUser();
        FetchedCertificateImage image = imageFetcher.fetch(request.getImageUrl());
        CertificateAiScanResult aiResult = certificateAiClient.scan(
                image.getBytes(),
                image.getFilename(),
                student.getStudentCode(),
                student.getFullName());

        List<String> warnings = new ArrayList<>(aiResult.getWarnings() != null ? aiResult.getWarnings() : List.of());
        LocalDate issuedDate = parseDate(aiResult.getIssuedDate());
        Semesters semester = resolveSemester(request.getSemesterId(), issuedDate);
        CertificateCategorySuggestion suggestion = categorySuggestionService.suggest(aiResult, warnings);
        appendStudentMismatchWarnings(student, aiResult, warnings);
        boolean autoRejected = shouldAutoReject(student, aiResult);

        CertificateSubmission submission = new CertificateSubmission();
        submission.setStudent(student);
        submission.setStudentCodeSnapshot(student.getStudentCode());
        submission.setStudentNameSnapshot(student.getFullName());
        submission.setDepartmentId(student.getDepartmentId());
        submission.setSemester(semester);
        submission.setImageUrl(request.getImageUrl().trim());
        submission.setStudentNote(blankToNull(request.getStudentNote()));
        submission.setRawText(blankToNull(aiResult.getRawText()));
        submission.setExtractedJson(aiResult.getExtractedJson());
        submission.setExtractedStudentName(blankToNull(aiResult.getStudentName()));
        submission.setExtractedStudentCode(blankToNull(aiResult.getStudentCode()));
        submission.setCertificateTitle(blankToNull(aiResult.getCertificateTitle()));
        submission.setIssuer(blankToNull(aiResult.getIssuer()));
        submission.setIssuedDate(issuedDate);
        submission.setAchievement(blankToNull(aiResult.getAchievement()));
        submission.setSuggestedCategory(suggestion.category());
        submission.setSuggestedCategoryName(blankToNull(suggestion.categoryName()));
        submission.setSuggestedPoint(suggestion.point());
        submission.setSuggestionReason(blankToNull(suggestion.reason()));
        submission.setAiConfidence(aiResult.getConfidence());
        submission.setAiWarningsJson(toWarningsJson(warnings));
        submission.setNeedsReview(!autoRejected && (aiResult.getNeedsReview() == null || aiResult.getNeedsReview() || !warnings.isEmpty()));
        submission.setStatus(autoRejected ? CertificateSubmission.STATUS_REJECTED : CertificateSubmission.STATUS_PENDING);
        if (autoRejected) {
            submission.setReviewedAt(LocalDateTime.now());
            submission.setRejectionReason(buildAutoRejectReason(student, aiResult));
        }

        CertificateSubmission saved = certificateSubmissionRepository.save(submission);
        if (autoRejected) {
            eventProducer.publishRejected(saved);
        } else {
            eventProducer.publishSubmitted(saved);
        }
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageDTO<CertificateSubmissionResponse> getMySubmissions(Long semesterId, Integer status, Pageable pageable) {
        Users student = getCurrentUser();
        Page<CertificateSubmission> page = certificateSubmissionRepository.findStudentSubmissions(
                student.getId(), semesterId, status, pageable);
        return toPageDto(page);
    }

    @Override
    @Transactional(readOnly = true)
    public PageDTO<CertificateSubmissionResponse> getReviewSubmissions(
            Integer status,
            Long departmentId,
            Long semesterId,
        String keyword,
        Pageable pageable) {
        Users reviewer = getCurrentUser();
        Long scopedDepartmentId = isAdmin() ? departmentId : requireReviewerDepartmentId(reviewer);
        Page<CertificateSubmission> page = certificateSubmissionRepository.findReviewSubmissions(
                scopedDepartmentId, semesterId, status, blankToNull(keyword), pageable);
        return toPageDto(page);
    }

    @Override
    @Transactional(readOnly = true)
    public CertificateSubmissionResponse getById(Long id) {
        CertificateSubmission submission = findDetailed(id);
        ensureCanView(submission);
        return mapper.toResponse(submission);
    }

    @Override
    @Transactional
    public CertificateSubmissionResponse approve(Long id, CertificateSubmissionReviewRequest request) {
        CertificateSubmission submission = findDetailed(id);
        ensureCanReview(submission);
        if (Objects.equals(submission.getStatus(), CertificateSubmission.STATUS_APPROVED)) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Hồ sơ giấy khen đã được duyệt.");
        }
        if (Objects.equals(submission.getStatus(), CertificateSubmission.STATUS_CANCELLED)) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Hồ sơ giấy khen đã bị hủy.");
        }

        Categories approvedCategory = validateApprovedCategory(request.getApprovedCategoryId(), request.getApprovedPoint());
        Users reviewer = getCurrentUser();

        submission.setStatus(CertificateSubmission.STATUS_APPROVED);
        submission.setReviewer(reviewer);
        submission.setReviewedAt(LocalDateTime.now());
        submission.setApprovedCategory(approvedCategory);
        submission.setApprovedPoint(request.getApprovedPoint());
        String adjustedCertificateTitle = blankToNull(request.getCertificateTitle());
        if (adjustedCertificateTitle != null) {
            submission.setCertificateTitle(adjustedCertificateTitle);
        }
        String adjustedAchievement = blankToNull(request.getAchievement());
        if (adjustedAchievement != null) {
            submission.setAchievement(adjustedAchievement);
        }
        submission.setReviewNote(blankToNull(request.getReviewNote()));
        submission.setRejectionReason(null);

        CertificateSubmission saved = certificateSubmissionRepository.save(submission);
        eventProducer.publishApproved(saved);
        pointEventProducer.publishCertificateAwarded(
                saved.getStudent() != null ? saved.getStudent().getId() : null,
                saved.getSemester() != null ? saved.getSemester().getId() : null,
                saved.getCertificateTitle(),
                saved.getApprovedPoint());
        pointCacheService.evictStudentPointCaches(
                saved.getStudent() != null ? saved.getStudent().getId() : null,
                saved.getSemester() != null ? saved.getSemester().getId() : null);
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    public CertificateSubmissionResponse reject(Long id, CertificateSubmissionRejectRequest request) {
        CertificateSubmission submission = findDetailed(id);
        ensureCanReview(submission);
        if (Objects.equals(submission.getStatus(), CertificateSubmission.STATUS_APPROVED)) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Không thể từ chối hồ sơ giấy khen đã được duyệt.");
        }
        if (request.getReason() == null || request.getReason().isBlank()) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Vui lòng nhập lý do từ chối.");
        }

        Users reviewer = getCurrentUser();
        submission.setStatus(CertificateSubmission.STATUS_REJECTED);
        submission.setReviewer(reviewer);
        submission.setReviewedAt(LocalDateTime.now());
        submission.setApprovedCategory(null);
        submission.setApprovedPoint(null);
        submission.setReviewNote(null);
        submission.setRejectionReason(request.getReason().trim());

        CertificateSubmission saved = certificateSubmissionRepository.save(submission);
        eventProducer.publishRejected(saved);
        return mapper.toResponse(saved);
    }

    private PageDTO<CertificateSubmissionResponse> toPageDto(Page<CertificateSubmission> page) {
        return new PageDTO<>(page, page.getContent().stream().map(mapper::toResponse).toList());
    }

    private CertificateSubmission findDetailed(Long id) {
        return certificateSubmissionRepository.findDetailedById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Không tìm thấy hồ sơ giấy khen."));
    }

    private Users getCurrentUser() {
        String username = Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    private boolean isAdmin() {
        return hasRole("ROLE_ADMIN");
    }

    private boolean isDepartment() {
        return hasRole("ROLE_DEPARTMENT");
    }

    private boolean isStudent() {
        return hasRole("ROLE_STUDENT");
    }

    private boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> role.equals(authority.getAuthority()));
    }

    private void ensureCanView(CertificateSubmission submission) {
        Users currentUser = getCurrentUser();
        if (isAdmin()) {
            return;
        }
        if (isStudent() && submission.getStudent() != null
                && Objects.equals(submission.getStudent().getId(), currentUser.getId())) {
            return;
        }
        if (isDepartment()
                && submission.getDepartmentId() != null
                && Objects.equals(submission.getDepartmentId(), requireReviewerDepartmentId(currentUser))) {
            return;
        }
        throw new AppException(ErrorCode.FORBIDDEN, "Bạn không có quyền xem hồ sơ giấy khen này.");
    }

    private void ensureCanReview(CertificateSubmission submission) {
        if (isAdmin()) {
            return;
        }
        Users reviewer = getCurrentUser();
        if (isDepartment()
                && submission.getDepartmentId() != null
                && Objects.equals(submission.getDepartmentId(), requireReviewerDepartmentId(reviewer))) {
            return;
        }
        throw new AppException(ErrorCode.FORBIDDEN, "Bạn không có quyền duyệt hồ sơ giấy khen này.");
    }

    private Long requireReviewerDepartmentId(Users reviewer) {
        if (reviewer.getDepartmentId() == null) {
            throw new AppException(ErrorCode.FORBIDDEN, "Tài khoản duyệt chưa được gắn đơn vị.");
        }
        return reviewer.getDepartmentId();
    }

    private Semesters resolveSemester(Long semesterId, LocalDate issuedDate) {
        if (semesterId != null) {
            return semesterRepository.findById(semesterId)
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Không tìm thấy học kỳ."));
        }
        if (issuedDate != null) {
            return semesterRepository.findSemesterByDate(issuedDate)
                    .or(() -> semesterRepository.findByIsActiveTrue())
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Không có học kỳ phù hợp."));
        }
        return semesterRepository.findByIsActiveTrue()
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Không có học kỳ hiện tại."));
    }

    private Categories validateApprovedCategory(Long categoryId, Integer point) {
        if (categoryId == null) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Vui lòng chọn tiêu chí điểm rèn luyện.");
        }
        if (point == null || point < 0) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Điểm được duyệt phải lớn hơn hoặc bằng 0.");
        }
        Categories category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED,
                        "Không tìm thấy tiêu chí điểm rèn luyện."));
        if (!Boolean.TRUE.equals(category.getIsActive())) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Tiêu chí điểm rèn luyện đã ngừng sử dụng.");
        }
        if (categoryRepository.existsByParentIdAndIsActive(categoryId, true)) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Chỉ được duyệt vào tiêu chí nhỏ nhất.");
        }
        int maxPoint = category.getMaxPoint() == null ? 0 : category.getMaxPoint();
        if (maxPoint > 0 && point > maxPoint) {
            throw new AppException(ErrorCode.INVALID_ACTION,
                    "Điểm được duyệt không được vượt quá " + maxPoint + " điểm của tiêu chí đã chọn.");
        }
        return category;
    }

    private void appendStudentMismatchWarnings(Users student, CertificateAiScanResult aiResult, List<String> warnings) {
        if (aiResult.getStudentCode() != null
                && student.getStudentCode() != null
                && !student.getStudentCode().equalsIgnoreCase(aiResult.getStudentCode().trim())) {
            warnings.add("MSSV trên giấy khen không khớp với tài khoản sinh viên.");
        }
        if (aiResult.getStudentName() != null
                && student.getFullName() != null
                && !normalizeName(aiResult.getStudentName()).contains(normalizeName(student.getFullName()))
                && !normalizeName(student.getFullName()).contains(normalizeName(aiResult.getStudentName()))) {
            warnings.add("Họ tên trên giấy khen cần được người duyệt kiểm tra lại.");
        }
    }

    private boolean shouldAutoReject(Users student, CertificateAiScanResult aiResult) {
        if (aiResult == null) {
            return true;
        }

        return hasStudentNameMismatch(student, aiResult) || !hasStudentUnitEvidence(aiResult);
    }

    private boolean hasStudentNameMismatch(Users student, CertificateAiScanResult aiResult) {
        if (student == null
                || student.getFullName() == null
                || aiResult.getStudentName() == null
                || aiResult.getStudentName().isBlank()) {
            return false;
        }

        String extracted = normalizeName(aiResult.getStudentName());
        String expected = normalizeName(student.getFullName());
        return !extracted.contains(expected) && !expected.contains(extracted);
    }

    private boolean hasStudentUnitEvidence(CertificateAiScanResult aiResult) {
        return containsStudentUnitTerm(aiResult.getRawText())
                || containsStudentUnitTerm(aiResult.getCertificateTitle())
                || containsStudentUnitTerm(aiResult.getAchievement())
                || containsStudentUnitTerm(aiResult.getIssuer());
    }

    private boolean containsStudentUnitTerm(String value) {
        String normalized = normalizeName(value);
        return normalized.contains("chi doan")
                || normalized.contains("lien chi doan")
                || normalized.contains("chi doan sinh vien")
                || normalized.contains("chi hoi")
                || normalized.contains("lien chi hoi")
                || normalized.contains("nganh")
                || normalized.contains("chuyen nganh")
                || normalized.contains("lop")
                || normalized.contains("khoa")
                || normalized.matches(".*\\bclc\\s*k\\d{2}\\b.*")
                || normalized.matches(".*\\bk\\d{2}\\b.*");
    }

    private String buildAutoRejectReason(Users student, CertificateAiScanResult aiResult) {
        List<String> reasons = new ArrayList<>();
        if (hasStudentNameMismatch(student, aiResult)) {
            reasons.add("họ tên không khớp");
        }
        if (!hasStudentUnitEvidence(aiResult)) {
            reasons.add("không tìm thấy thông tin chi đoàn/ngành/lớp");
        }

        if (reasons.isEmpty()) {
            reasons.add("thông tin minh chứng không đạt yêu cầu");
        }

        return "Từ chối tự động do " + String.join(", ", reasons) + ".";
    }

    private String normalizeName(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return normalized;
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private String toWarningsJson(List<String> warnings) {
        try {
            return objectMapper.writeValueAsString(warnings != null ? warnings : List.of());
        } catch (Exception exception) {
            return "[]";
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
