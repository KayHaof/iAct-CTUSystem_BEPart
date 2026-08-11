package com.example.activityservice.feature.certificate_submissions.kafka;

import com.example.activityservice.feature.certificate_submissions.model.CertificateSubmission;
import com.example.activityservice.feature.kafka.KafkaEnvelopePublisher;
import com.example.event.kafka.KafkaEventTypes;
import com.example.event.kafka.KafkaTopics;
import com.example.util.UtcDateTime;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class CertificateSubmissionEventProducer extends KafkaEnvelopePublisher {

    public CertificateSubmissionEventProducer(KafkaTemplate<String, Object> kafkaTemplate, ObjectMapper objectMapper) {
        super(kafkaTemplate, objectMapper, "activity-service");
    }

    public void publishSubmitted(CertificateSubmission submission) {
        publish(KafkaTopics.CERTIFICATE_SUBMISSION_SUBMITTED,
                KafkaEventTypes.CERTIFICATE_SUBMISSION_SUBMITTED,
                "certificate-submission",
                String.valueOf(submission.getId()),
                payload(submission));
    }

    public void publishApproved(CertificateSubmission submission) {
        publish(KafkaTopics.CERTIFICATE_SUBMISSION_APPROVED,
                KafkaEventTypes.CERTIFICATE_SUBMISSION_APPROVED,
                "certificate-submission",
                String.valueOf(submission.getId()),
                payload(submission));
    }

    public void publishRejected(CertificateSubmission submission) {
        publish(KafkaTopics.CERTIFICATE_SUBMISSION_REJECTED,
                KafkaEventTypes.CERTIFICATE_SUBMISSION_REJECTED,
                "certificate-submission",
                String.valueOf(submission.getId()),
                payload(submission));
    }

    private Map<String, Object> payload(CertificateSubmission submission) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("submissionId", submission.getId());
        payload.put("userId", submission.getStudent() != null ? submission.getStudent().getId() : null);
        payload.put("studentCode", submission.getStudentCodeSnapshot());
        payload.put("studentName", submission.getStudentNameSnapshot());
        payload.put("departmentId", submission.getDepartmentId());
        payload.put("semesterId", submission.getSemester() != null ? submission.getSemester().getId() : null);
        payload.put("certificateTitle", submission.getCertificateTitle());
        payload.put("status", submission.getStatus());
        payload.put("approvedPoint", submission.getApprovedPoint());
        payload.put("reason", submission.getRejectionReason());
        payload.put("reviewerId", submission.getReviewer() != null ? submission.getReviewer().getId() : null);
        payload.put("reviewedAt", UtcDateTime.format(submission.getReviewedAt()));
        return payload;
    }
}
