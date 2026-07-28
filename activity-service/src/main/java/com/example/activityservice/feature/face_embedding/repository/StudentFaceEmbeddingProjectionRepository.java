package com.example.activityservice.feature.face_embedding.repository;

import com.example.activityservice.feature.face_embedding.model.StudentFaceEmbeddingProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StudentFaceEmbeddingProjectionRepository extends JpaRepository<StudentFaceEmbeddingProjection, Long> {

    @Query("SELECT embedding FROM StudentFaceEmbeddingProjection embedding " +
            "WHERE embedding.userId = :userId AND embedding.status = 1")
    Optional<StudentFaceEmbeddingProjection> findActiveByUserId(@Param("userId") Long userId);
}
