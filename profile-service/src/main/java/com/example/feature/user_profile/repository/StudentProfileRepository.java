package com.example.feature.user_profile.repository;

import com.example.feature.user_profile.model.StudentProfile;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long> {

    @Query("SELECT s.userId FROM StudentProfile s WHERE " +
            "(:keyword IS NULL OR LOWER(s.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(s.studentCode) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Long> searchIdsByKeyword(@Param("keyword") String keyword);

    @EntityGraph(attributePaths = {"clazz", "clazz.major", "clazz.major.department"})
    Optional<StudentProfile> findById(Long userId);

    @Query("SELECT s.userId FROM StudentProfile s " +
            "LEFT JOIN s.clazz c " +
            "LEFT JOIN c.major m " +
            "LEFT JOIN m.department d " +
            "WHERE (:keyword IS NULL OR :keyword = '' OR LOWER(s.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(s.studentCode) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:departmentId IS NULL OR d.id = :departmentId) " +
            "AND (:classId IS NULL OR (:classId = 0 AND c IS NULL) OR (c.id = :classId))")
    List<Long> searchIdsByCriteria(
            @Param("keyword") String keyword,
            @Param("departmentId") Long departmentId,
            @Param("classId") Long classId
    );

    @Query("SELECT s.studentCode FROM StudentProfile s WHERE s.studentCode IN :codes")
    Set<String> findExistingStudentCodes(@Param("codes") Collection<String> codes);
}