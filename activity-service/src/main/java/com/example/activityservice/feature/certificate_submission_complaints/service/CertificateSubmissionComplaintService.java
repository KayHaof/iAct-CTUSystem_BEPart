package com.example.activityservice.feature.certificate_submission_complaints.service;

import com.example.activityservice.feature.certificate_submission_complaints.dto.CertificateSubmissionComplaintApproveRequest;
import com.example.activityservice.feature.certificate_submission_complaints.dto.CertificateSubmissionComplaintRejectRequest;
import com.example.activityservice.feature.certificate_submission_complaints.dto.CertificateSubmissionComplaintRequest;
import com.example.activityservice.feature.certificate_submission_complaints.dto.CertificateSubmissionComplaintResponse;
import com.example.dto.PageDTO;
import org.springframework.data.domain.Pageable;

public interface CertificateSubmissionComplaintService {
    CertificateSubmissionComplaintResponse submit(CertificateSubmissionComplaintRequest request);

    PageDTO<CertificateSubmissionComplaintResponse> getMyComplaints(Long semesterId, Integer status, Pageable pageable);

    PageDTO<CertificateSubmissionComplaintResponse> getReviewComplaints(
            Integer status,
            Long departmentId,
            Long semesterId,
            String keyword,
            Pageable pageable);

    CertificateSubmissionComplaintResponse getById(Long id);

    CertificateSubmissionComplaintResponse approve(Long id, CertificateSubmissionComplaintApproveRequest request);

    CertificateSubmissionComplaintResponse reject(Long id, CertificateSubmissionComplaintRejectRequest request);
}
