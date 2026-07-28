package com.example.userservice.feature.user_profile.service;

import com.example.userservice.feature.user_profile.dto.StudentFaceEmbeddingRequest;
import com.example.userservice.feature.user_profile.dto.StudentFaceEmbeddingResponse;

public interface StudentFaceEmbeddingService {
    StudentFaceEmbeddingResponse upsert(Long userId, StudentFaceEmbeddingRequest request);

    StudentFaceEmbeddingResponse getActive(Long userId);

    StudentFaceEmbeddingResponse revoke(Long userId, String reason);

    long replayAll();
}
