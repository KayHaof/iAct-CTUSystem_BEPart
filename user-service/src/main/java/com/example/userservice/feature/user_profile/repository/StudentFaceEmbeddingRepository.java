package com.example.userservice.feature.user_profile.repository;

import com.example.userservice.feature.user_profile.model.StudentFaceEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StudentFaceEmbeddingRepository extends JpaRepository<StudentFaceEmbedding, Long> {

    @Query("SELECT embedding FROM StudentFaceEmbedding embedding " +
            "WHERE embedding.userId = :userId AND embedding.status = 1")
    Optional<StudentFaceEmbedding> findActiveByUserId(@Param("userId") Long userId);

    boolean existsByUserIdAndStatus(Long userId, Integer status);
}
