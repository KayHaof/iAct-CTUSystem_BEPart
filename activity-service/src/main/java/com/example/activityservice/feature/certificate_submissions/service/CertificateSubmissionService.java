package com.example.activityservice.feature.certificate_submissions.service;

import com.example.activityservice.feature.certificate_submissions.dto.CertificateSubmissionRejectRequest;
import com.example.activityservice.feature.certificate_submissions.dto.CertificateSubmissionRequest;
import com.example.activityservice.feature.certificate_submissions.dto.CertificateSubmissionResponse;
import com.example.activityservice.feature.certificate_submissions.dto.CertificateSubmissionReviewRequest;
import com.example.dto.PageDTO;
import org.springframework.data.domain.Pageable;

public interface CertificateSubmissionService {
    CertificateSubmissionResponse submit(CertificateSubmissionRequest request);

    PageDTO<CertificateSubmissionResponse> getMySubmissions(Long semesterId, Integer status, Pageable pageable);

    PageDTO<CertificateSubmissionResponse> getReviewSubmissions(
            Integer status,
            Long departmentId,
            Long semesterId,
            String keyword,
            boolean excludeAutoRejected,
            Pageable pageable);

    CertificateSubmissionResponse getById(Long id);

    CertificateSubmissionResponse approve(Long id, CertificateSubmissionReviewRequest request);

    CertificateSubmissionResponse reject(Long id, CertificateSubmissionRejectRequest request);
}
