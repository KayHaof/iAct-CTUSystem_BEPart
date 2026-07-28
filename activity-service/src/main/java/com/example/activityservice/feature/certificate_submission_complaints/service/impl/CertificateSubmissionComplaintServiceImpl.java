package com.example.activityservice.feature.certificate_submission_complaints.service.impl;

import com.example.activityservice.common.dto.NotificationRequest;
import com.example.activityservice.feature.categories.model.Categories;
import com.example.activityservice.feature.categories.repository.CategoryRepository;
import com.example.activityservice.feature.certificate_submission_complaints.dto.CertificateSubmissionComplaintApproveRequest;
import com.example.activityservice.feature.certificate_submission_complaints.dto.CertificateSubmissionComplaintRejectRequest;
import com.example.activityservice.feature.certificate_submission_complaints.dto.CertificateSubmissionComplaintRequest;
import com.example.activityservice.feature.certificate_submission_complaints.dto.CertificateSubmissionComplaintResponse;
import com.example.activityservice.feature.certificate_submission_complaints.mapper.CertificateSubmissionComplaintMapper;
import com.example.activityservice.feature.certificate_submission_complaints.model.CertificateSubmissionComplaint;
import com.example.activityservice.feature.certificate_submission_complaints.repository.CertificateSubmissionComplaintRepository;
import com.example.activityservice.feature.certificate_submission_complaints.service.CertificateSubmissionComplaintService;
import com.example.activityservice.feature.certificate_submissions.kafka.CertificateSubmissionEventProducer;
import com.example.activityservice.feature.certificate_submissions.model.CertificateSubmission;
import com.example.activityservice.feature.certificate_submissions.repository.CertificateSubmissionRepository;
import com.example.activityservice.feature.notification.kafka.NotificationCommandProducer;
import com.example.activityservice.feature.points.kafka.PointEventProducer;
import com.example.activityservice.feature.points.service.PointCacheService;
import com.example.activityservice.feature.users.model.Users;
import com.example.activityservice.feature.users.repository.UserRepository;
import com.example.dto.PageDTO;
import com.example.exception.AppException;
import com.example.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CertificateSubmissionComplaintServiceImpl implements CertificateSubmissionComplaintService {

    private final CertificateSubmissionComplaintRepository complaintRepository;
    private final CertificateSubmissionRepository certificateSubmissionRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final CertificateSubmissionComplaintMapper mapper;
    private final CertificateSubmissionEventProducer eventProducer;
    private final PointEventProducer pointEventProducer;
    private final PointCacheService pointCacheService;
    private final NotificationCommandProducer notificationCommandProducer;

    @Override
    @Transactional
    public CertificateSubmissionComplaintResponse submit(CertificateSubmissionComplaintRequest request) {
        Users student = getCurrentUser();
        CertificateSubmission submission = findSubmission(request.getSubmissionId());
        ensureCanAppeal(student, submission);

        if (complaintRepository.existsByCertificateSubmissionId(submission.getId())) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Ho so nay da co khiieu nai.");
        }

        CertificateSubmissionComplaint complaint = new CertificateSubmissionComplaint();
        complaint.setCertificateSubmission(submission);
        complaint.setComplaintReason(blankToNull(request.getComplaintReason()));
        complaint.setStatus(CertificateSubmissionComplaint.STATUS_PENDING);

        return mapper.toResponse(complaintRepository.save(complaint));
    }

    @Override
    @Transactional(readOnly = true)
    public PageDTO<CertificateSubmissionComplaintResponse> getMyComplaints(Long semesterId, Integer status, Pageable pageable) {
        Users student = getCurrentUser();
        Page<CertificateSubmissionComplaint> page = complaintRepository.findMyComplaints(
                student.getId(),
                semesterId,
                status,
                pageable);
        return new PageDTO<>(page, page.getContent().stream().map(mapper::toResponse).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PageDTO<CertificateSubmissionComplaintResponse> getReviewComplaints(
            Integer status,
            Long departmentId,
            Long semesterId,
            String keyword,
            Pageable pageable) {
        Long scopedDepartmentId = isAdmin() ? departmentId : requireReviewerDepartmentId(getCurrentUser());
        Page<CertificateSubmissionComplaint> page = complaintRepository.findReviewComplaints(
                scopedDepartmentId,
                semesterId,
                status,
                normalizeKeyword(keyword),
                pageable);
        return new PageDTO<>(page, page.getContent().stream().map(mapper::toResponse).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CertificateSubmissionComplaintResponse getById(Long id) {
        CertificateSubmissionComplaint complaint = findDetailed(id);
        ensureCanView(complaint);
        return mapper.toResponse(complaint);
    }

    @Override
    @Transactional
    public CertificateSubmissionComplaintResponse approve(Long id, CertificateSubmissionComplaintApproveRequest request) {
        CertificateSubmissionComplaint complaint = findDetailed(id);
        ensureCanReview(complaint);
        ensurePending(complaint);
        ensureAppealableSubmission(complaint.getCertificateSubmission());

        Categories approvedCategory = validateApprovedCategory(request.getApprovedCategoryId(), request.getApprovedPoint());
        Users reviewer = getCurrentUser();
        LocalDateTime now = LocalDateTime.now();

        complaint.setStatus(CertificateSubmissionComplaint.STATUS_APPROVED);
        complaint.setReviewer(reviewer);
        complaint.setReviewedAt(now);
        complaint.setReviewNote(blankToNull(request.getReviewNote()));
        complaint.setRejectionReason(null);

        CertificateSubmission submission = complaint.getCertificateSubmission();
        submission.setStatus(CertificateSubmission.STATUS_APPROVED);
        submission.setReviewer(reviewer);
        submission.setReviewedAt(now);
        submission.setApprovedCategory(approvedCategory);
        submission.setApprovedPoint(request.getApprovedPoint());
        submission.setReviewNote(blankToNull(request.getReviewNote()));
        submission.setRejectionReason(null);
        submission.setNeedsReview(false);

        CertificateSubmission savedSubmission = certificateSubmissionRepository.save(submission);
        CertificateSubmissionComplaint savedComplaint = complaintRepository.save(complaint);

        eventProducer.publishApproved(savedSubmission);
        pointEventProducer.publishCertificateAwarded(
                savedSubmission.getStudent() != null ? savedSubmission.getStudent().getId() : null,
                savedSubmission.getSemester() != null ? savedSubmission.getSemester().getId() : null,
                savedSubmission.getCertificateTitle(),
                savedSubmission.getApprovedPoint());
        pointCacheService.evictStudentPointCaches(
                savedSubmission.getStudent() != null ? savedSubmission.getStudent().getId() : null,
                savedSubmission.getSemester() != null ? savedSubmission.getSemester().getId() : null);
        publishComplaintNotification(
                savedComplaint,
                "Khieu nai giay khen duoc duyet",
                buildApprovalMessage(savedSubmission),
                1,
                "certificate-submission-complaint-approved");
        return mapper.toResponse(savedComplaint);
    }

    @Override
    @Transactional
    public CertificateSubmissionComplaintResponse reject(Long id, CertificateSubmissionComplaintRejectRequest request) {
        CertificateSubmissionComplaint complaint = findDetailed(id);
        ensureCanReview(complaint);
        ensurePending(complaint);
        ensureAppealableSubmission(complaint.getCertificateSubmission());

        Users reviewer = getCurrentUser();
        LocalDateTime now = LocalDateTime.now();

        complaint.setStatus(CertificateSubmissionComplaint.STATUS_REJECTED);
        complaint.setReviewer(reviewer);
        complaint.setReviewedAt(now);
        complaint.setReviewNote(null);
        complaint.setRejectionReason(blankToNull(request.getRejectionReason()));

        CertificateSubmissionComplaint savedComplaint = complaintRepository.save(complaint);
        publishComplaintNotification(
                savedComplaint,
                "Khieu nai giay khen bi tu choi",
                request.getRejectionReason().trim(),
                3,
                "certificate-submission-complaint-rejected");
        return mapper.toResponse(savedComplaint);
    }

    private CertificateSubmission findSubmission(Long id) {
        return certificateSubmissionRepository.findDetailedById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Khong tim thay ho so giay khen."));
    }

    private CertificateSubmissionComplaint findDetailed(Long id) {
        return complaintRepository.findDetailById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Khong tim thay khiieu nai giay khen."));
    }

    private void ensureCanAppeal(Users student, CertificateSubmission submission) {
        if (submission.getStudent() == null || !Objects.equals(submission.getStudent().getId(), student.getId())) {
            throw new AppException(ErrorCode.FORBIDDEN, "Ban khong co quyen khiieu nai ho so nay.");
        }
        if (!Objects.equals(submission.getStatus(), CertificateSubmission.STATUS_REJECTED)) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Chi co the khiieu nai ho so bi tu choi.");
        }
        if (submission.getReviewer() != null) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Ho so nay da duoc truong xu ly, khong thuoc luong khiieu nai tu dong.");
        }
    }

    private void ensureAppealableSubmission(CertificateSubmission submission) {
        if (submission == null || !Objects.equals(submission.getStatus(), CertificateSubmission.STATUS_REJECTED)) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Ho so khiieu nai khong con hop le.");
        }
        if (submission.getReviewer() != null) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Ho so nay da duoc xu ly boi truong.");
        }
    }

    private void ensureCanView(CertificateSubmissionComplaint complaint) {
        Users currentUser = getCurrentUser();
        if (isAdmin()) {
            return;
        }
        CertificateSubmission submission = complaint.getCertificateSubmission();
        if (isStudent()
                && submission != null
                && submission.getStudent() != null
                && Objects.equals(submission.getStudent().getId(), currentUser.getId())) {
            return;
        }
        if (isDepartment()
                && submission != null
                && submission.getDepartmentId() != null
                && Objects.equals(submission.getDepartmentId(), requireReviewerDepartmentId(currentUser))) {
            return;
        }
        throw new AppException(ErrorCode.FORBIDDEN, "Ban khong co quyen xem khiieu nai nay.");
    }

    private void ensureCanReview(CertificateSubmissionComplaint complaint) {
        if (isAdmin()) {
            return;
        }
        Users reviewer = getCurrentUser();
        CertificateSubmission submission = complaint.getCertificateSubmission();
        if (isDepartment()
                && submission != null
                && submission.getDepartmentId() != null
                && Objects.equals(submission.getDepartmentId(), requireReviewerDepartmentId(reviewer))) {
            return;
        }
        throw new AppException(ErrorCode.FORBIDDEN, "Ban khong co quyen xu ly khiieu nai nay.");
    }

    private void ensurePending(CertificateSubmissionComplaint complaint) {
        if (complaint.getStatus() != null && complaint.getStatus() != CertificateSubmissionComplaint.STATUS_PENDING) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Khieu nai da duoc xu ly.");
        }
    }

    private Categories validateApprovedCategory(Long categoryId, Integer point) {
        if (categoryId == null) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Vui long chon tieu chi diem ren luyen.");
        }
        if (point == null || point < 0) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Diem duoc duyet phai lon hon hoac bang 0.");
        }
        Categories category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED,
                        "Khong tim thay tieu chi diem ren luyen."));
        if (!Boolean.TRUE.equals(category.getIsActive())) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Tieu chi diem ren luyen da ngung su dung.");
        }
        if (categoryRepository.existsByParentIdAndIsActive(categoryId, true)) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Chi duoc duyet vao tieu chi nho nhat.");
        }
        int maxPoint = category.getMaxPoint() == null ? 0 : category.getMaxPoint();
        if (maxPoint > 0 && point > maxPoint) {
            throw new AppException(ErrorCode.INVALID_ACTION,
                    "Diem duoc duyet khong duoc vuot qua " + maxPoint + " diem cua tieu chi da chon.");
        }
        return category;
    }

    private void publishComplaintNotification(
            CertificateSubmissionComplaint complaint,
            String title,
            String message,
            Integer type,
            String sourceEventId) {
        CertificateSubmission submission = complaint.getCertificateSubmission();
        Users student = submission != null ? submission.getStudent() : null;
        if (student == null || student.getId() == null) {
            return;
        }

        NotificationRequest notification = new NotificationRequest();
        notification.setUserId(student.getId());
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setContent(message);
        notification.setType(type);
        notification.setReferenceType("certificate-submission-complaint");
        notification.setSourceEventId(sourceEventId + ":" + complaint.getId());
        notification.setSourceTopic("certificate-submission-complaints");

        try {
            notificationCommandProducer.publishCreated(notification);
        } catch (Exception exception) {
            // keep complaint flow working even if notification fails
        }
    }

    private String buildApprovalMessage(CertificateSubmission submission) {
        StringBuilder builder = new StringBuilder("Khieu nai cua ban da duoc duyet");
        if (submission.getApprovedCategory() != null) {
            builder.append(" va da duoc ghi nhan vao ").append(submission.getApprovedCategory().getName());
        }
        if (submission.getApprovedPoint() != null) {
            builder.append(" voi ").append(submission.getApprovedPoint()).append(" diem.");
        }
        return builder.toString();
    }

    private Long requireReviewerDepartmentId(Users reviewer) {
        if (reviewer == null || reviewer.getDepartmentId() == null) {
            throw new AppException(ErrorCode.FORBIDDEN, "Tai khoan xu ly chua duoc gan don vi.");
        }
        return reviewer.getDepartmentId();
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

    private String normalizeKeyword(String keyword) {
        return keyword == null || keyword.isBlank() ? null : keyword.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
