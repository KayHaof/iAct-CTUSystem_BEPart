package com.example.common.repository;

import com.example.common.model.IdtLocalProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface LocalProfileRepository extends JpaRepository<IdtLocalProfile, Long> {
    Optional<IdtLocalProfile> findByUserId(Long userId);

    List<IdtLocalProfile> findAllByUserIdIn(List<Long> userIds);

    @Query("SELECT p.userId FROM IdtLocalProfile p WHERE " +
            "(:keyword IS NULL OR LOWER(p.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR p.studentCode LIKE CONCAT('%', :keyword, '%')) " +
            "AND (:departmentId IS NULL OR p.departmentId = :departmentId) " +
            "AND (:classId IS NULL OR p.classId = :classId)")
    List<Long> findUserIdsByCriteria(@Param("keyword") String keyword,
                                     @Param("departmentId") Long departmentId,
                                     @Param("classId") Long classId);
}
