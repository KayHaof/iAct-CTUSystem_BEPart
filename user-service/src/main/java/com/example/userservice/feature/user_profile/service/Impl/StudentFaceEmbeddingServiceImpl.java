package com.example.userservice.feature.user_profile.service.Impl;

import com.example.exception.AppException;
import com.example.exception.ErrorCode;
import com.example.userservice.feature.user_profile.ai.AiFaceEmbeddingClient;
import com.example.userservice.feature.user_profile.ai.FaceEmbeddingExtractionResult;
import com.example.userservice.feature.user_profile.dto.StudentFaceEmbeddingRequest;
import com.example.userservice.feature.user_profile.dto.StudentFaceEmbeddingResponse;
import com.example.userservice.feature.user_profile.kafka.StudentFaceEmbeddingEventProducer;
import com.example.userservice.feature.user_profile.model.StudentFaceEmbedding;
import com.example.userservice.feature.user_profile.repository.StudentFaceEmbeddingRepository;
import com.example.userservice.feature.user_profile.repository.StudentProfileRepository;
import com.example.userservice.feature.user_profile.service.StudentFaceEmbeddingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class StudentFaceEmbeddingServiceImpl implements StudentFaceEmbeddingService {

    private static final int REPLAY_PAGE_SIZE = 100;

    private final StudentFaceEmbeddingRepository embeddingRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final StudentFaceEmbeddingEventProducer eventProducer;
    private final AiFaceEmbeddingClient aiFaceEmbeddingClient;

    @Override
    @Transactional
    public StudentFaceEmbeddingResponse upsert(Long userId, StudentFaceEmbeddingRequest request) {
        ensureStudentProfileExists(userId);
        if (!hasText(request.getReferenceImageUrl())) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Thieu URL anh goc tren Cloudinary");
        }
        FaceEmbeddingExtractionResult extracted = resolveEmbeddingData(request);

        StudentFaceEmbedding embedding = embeddingRepository.findById(userId)
                .orElseGet(() -> {
                    StudentFaceEmbedding created = new StudentFaceEmbedding();
                    created.setUserId(userId);
                    return created;
                });

        embedding.setReferenceImageUrl(request.getReferenceImageUrl());
        embedding.setReferenceImagePublicId(request.getReferenceImagePublicId());
        embedding.setEmbeddingVector(extracted.getEmbeddingVector());
        embedding.setVectorSize(extracted.getVectorSize());
        embedding.setModelName(extracted.getModelName());
        embedding.setDetectorBackend(extracted.getDetectorBackend());
        embedding.setNormalizationMethod(extracted.getNormalizationMethod());
        embedding.setDistanceMetric(
                request.getDistanceMetric() != null && !request.getDistanceMetric().isBlank()
                        ? request.getDistanceMetric()
                        : "cosine");
        embedding.setQualityScore(extracted.getQualityScore());
        embedding.setFaceConfidence(extracted.getFaceConfidence());
        embedding.setEmbeddingVersion(resolveEmbeddingVersion(embedding, request));
        embedding.setStatus(StudentFaceEmbedding.STATUS_ACTIVE);
        embedding.setRevokedAt(null);
        embedding.setRevokedReason(null);

        StudentFaceEmbedding saved = embeddingRepository.save(embedding);
        eventProducer.publishUpserted(saved);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public StudentFaceEmbeddingResponse getActive(Long userId) {
        return embeddingRepository.findActiveByUserId(userId)
                .map(this::toResponse)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED,
                        "Sinh vien chua co vector khuon mat dang hoat dong"));
    }

    @Override
    @Transactional
    public StudentFaceEmbeddingResponse revoke(Long userId, String reason) {
        StudentFaceEmbedding embedding = embeddingRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED,
                        "Sinh vien chua co vector khuon mat"));
        embedding.setStatus(StudentFaceEmbedding.STATUS_REVOKED);
        embedding.setRevokedAt(LocalDateTime.now());
        embedding.setRevokedReason(reason != null && !reason.isBlank() ? reason : "Thu hoi vector khuon mat");

        StudentFaceEmbedding saved = embeddingRepository.save(embedding);
        eventProducer.publishRevoked(saved);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public long replayAll() {
        int pageNumber = 0;
        long scheduledEvents = 0;
        Page<StudentFaceEmbedding> page;
        do {
            page = embeddingRepository.findAll(PageRequest.of(
                    pageNumber++, REPLAY_PAGE_SIZE, Sort.by(Sort.Direction.ASC, "userId")));
            for (StudentFaceEmbedding embedding : page.getContent()) {
                if (Integer.valueOf(StudentFaceEmbedding.STATUS_REVOKED).equals(embedding.getStatus())) {
                    eventProducer.publishRevoked(embedding);
                } else {
                    eventProducer.publishUpserted(embedding);
                }
                scheduledEvents++;
            }
        } while (page.hasNext());
        return scheduledEvents;
    }

    private void ensureStudentProfileExists(Long userId) {
        if (!studentProfileRepository.existsById(userId)) {
            throw new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Khong tim thay ho so sinh vien");
        }
    }

    private FaceEmbeddingExtractionResult resolveEmbeddingData(StudentFaceEmbeddingRequest request) {
        if (!hasText(request.getEmbeddingVector())
                || request.getVectorSize() == null
                || !hasText(request.getModelName())) {
            return aiFaceEmbeddingClient.extractFromImageUrl(request.getReferenceImageUrl());
        }
        return FaceEmbeddingExtractionResult.builder()
                .embeddingVector(request.getEmbeddingVector())
                .vectorSize(request.getVectorSize())
                .modelName(request.getModelName())
                .detectorBackend(request.getDetectorBackend())
                .normalizationMethod(request.getNormalizationMethod())
                .qualityScore(request.getQualityScore())
                .faceConfidence(request.getFaceConfidence())
                .build();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private Integer resolveEmbeddingVersion(StudentFaceEmbedding embedding, StudentFaceEmbeddingRequest request) {
        if (request.getEmbeddingVersion() != null) {
            return request.getEmbeddingVersion();
        }
        if (embedding.getEmbeddingVersion() == null) {
            return 1;
        }
        return embedding.getEmbeddingVersion() + 1;
    }

    private StudentFaceEmbeddingResponse toResponse(StudentFaceEmbedding embedding) {
        return StudentFaceEmbeddingResponse.builder()
                .userId(embedding.getUserId())
                .referenceImageUrl(embedding.getReferenceImageUrl())
                .referenceImagePublicId(embedding.getReferenceImagePublicId())
                .embeddingVector(embedding.getEmbeddingVector())
                .vectorSize(embedding.getVectorSize())
                .modelName(embedding.getModelName())
                .detectorBackend(embedding.getDetectorBackend())
                .normalizationMethod(embedding.getNormalizationMethod())
                .distanceMetric(embedding.getDistanceMetric())
                .qualityScore(embedding.getQualityScore())
                .faceConfidence(embedding.getFaceConfidence())
                .embeddingVersion(embedding.getEmbeddingVersion())
                .status(embedding.getStatus())
                .lastVerifiedAt(embedding.getLastVerifiedAt())
                .createdAt(embedding.getCreatedAt())
                .updatedAt(embedding.getUpdatedAt())
                .revokedAt(embedding.getRevokedAt())
                .revokedReason(embedding.getRevokedReason())
                .build();
    }
}
