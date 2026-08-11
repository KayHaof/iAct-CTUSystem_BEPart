package com.example.activityservice.feature.face_embedding.service;

import com.example.activityservice.feature.face_embedding.model.StudentFaceEmbeddingProjection;
import com.example.activityservice.feature.face_embedding.repository.StudentFaceEmbeddingProjectionRepository;
import com.example.activityservice.feature.users.model.Users;
import com.example.activityservice.feature.users.repository.UserRepository;
import com.example.event.StudentFaceEmbeddingEvent;
import com.example.exception.AppException;
import com.example.exception.ErrorCode;
import com.example.util.UtcDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class StudentFaceEmbeddingProjectionService {

    private final StudentFaceEmbeddingProjectionRepository embeddingRepository;
    private final UserRepository userRepository;

    @Transactional
    public StudentFaceEmbeddingProjection upsert(StudentFaceEmbeddingEvent event) {
        if (event.getUserId() == null) {
            throw new IllegalArgumentException("Student face embedding event must contain userId");
        }

        ensureLocalUserExists(event.getUserId());
        StudentFaceEmbeddingProjection projection = embeddingRepository.findById(event.getUserId())
                .orElseGet(() -> {
                    StudentFaceEmbeddingProjection created = new StudentFaceEmbeddingProjection();
                    created.setUserId(event.getUserId());
                    return created;
                });

        projection.setReferenceImageUrl(event.getReferenceImageUrl());
        projection.setReferenceImagePublicId(event.getReferenceImagePublicId());
        projection.setEmbeddingVector(event.getEmbeddingVector());
        projection.setVectorSize(event.getVectorSize());
        projection.setModelName(event.getModelName());
        projection.setDetectorBackend(event.getDetectorBackend());
        projection.setNormalizationMethod(event.getNormalizationMethod());
        projection.setDistanceMetric(event.getDistanceMetric());
        projection.setQualityScore(event.getQualityScore());
        projection.setFaceConfidence(event.getFaceConfidence());
        projection.setEmbeddingVersion(event.getEmbeddingVersion());
        projection.setStatus(event.getStatus());
        projection.setLastVerifiedAt(parseDateTime(event.getLastVerifiedAt()));
        projection.setCreatedAt(parseDateTime(event.getCreatedAt()));
        projection.setUpdatedAt(parseDateTime(event.getUpdatedAt()));
        projection.setRevokedAt(parseDateTime(event.getRevokedAt()));
        projection.setRevokedReason(event.getRevokedReason());

        validateRequiredFields(projection);
        return embeddingRepository.save(projection);
    }

    @Transactional(readOnly = true)
    public StudentFaceEmbeddingProjection getActive(Long userId) {
        return embeddingRepository.findActiveByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_ACTION,
                        "Sinh viên chưa có vector khuôn mặt đang hoạt động"));
    }

    @Transactional(readOnly = true)
    public void ensureActiveForRegistration(Long userId) {
        if (embeddingRepository.findActiveByUserId(userId).isEmpty()) {
            throw new AppException(ErrorCode.INVALID_ACTION,
                    "Bạn cần nộp ảnh khuôn mặt trước khi đăng ký hoạt động");
        }
    }

    @Transactional
    public void markVerified(Long userId, LocalDateTime verifiedAt) {
        StudentFaceEmbeddingProjection projection = getActive(userId);
        projection.setLastVerifiedAt(verifiedAt != null ? verifiedAt : LocalDateTime.now());
        embeddingRepository.save(projection);
    }

    private void ensureLocalUserExists(Long userId) {
        if (userRepository.existsById(userId)) {
            return;
        }
        Users user = new Users();
        user.setId(userId);
        userRepository.save(user);
    }

    private void validateRequiredFields(StudentFaceEmbeddingProjection projection) {
        if (isBlank(projection.getReferenceImageUrl())
                || isBlank(projection.getEmbeddingVector())
                || projection.getVectorSize() == null
                || isBlank(projection.getModelName())) {
            throw new IllegalArgumentException("Student face embedding event does not contain required data");
        }
        if (isBlank(projection.getDistanceMetric())) {
            projection.setDistanceMetric("cosine");
        }
        if (projection.getEmbeddingVersion() == null) {
            projection.setEmbeddingVersion(1);
        }
        if (projection.getStatus() == null) {
            projection.setStatus(StudentFaceEmbeddingProjection.STATUS_ACTIVE);
        }
    }

    private LocalDateTime parseDateTime(String value) {
        return value == null || value.isBlank() ? null : UtcDateTime.parse(value);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
