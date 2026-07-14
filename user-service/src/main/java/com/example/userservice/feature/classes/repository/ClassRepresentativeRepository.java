package com.example.userservice.feature.classes.repository;

import com.example.userservice.feature.classes.model.ClassRepresentative;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ClassRepresentativeRepository extends JpaRepository<ClassRepresentative, Long> {

    @EntityGraph(attributePaths = {
            "clazz",
            "clazz.major",
            "clazz.major.department",
            "student"
    })
    @Query("""
            SELECT representative
            FROM ClassRepresentative representative
            WHERE representative.student.id = :studentId
              AND representative.isActive = true
              AND (representative.startDate IS NULL OR representative.startDate <= :today)
              AND (representative.endDate IS NULL OR representative.endDate >= :today)
            ORDER BY representative.id DESC
            """)
    List<ClassRepresentative> findActiveByStudentId(
            @Param("studentId") Long studentId,
            @Param("today") LocalDate today);

    @EntityGraph(attributePaths = {
            "clazz",
            "clazz.major",
            "clazz.major.department",
            "student"
    })
    @Query("""
            SELECT representative
            FROM ClassRepresentative representative
            WHERE (:departmentId IS NULL OR representative.clazz.major.department.id = :departmentId)
              AND (:classId IS NULL OR representative.clazz.id = :classId)
              AND (:active IS NULL OR representative.isActive = :active)
              AND (
                    :keyword IS NULL
                    OR :keyword = ''
                    OR LOWER(representative.student.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(representative.student.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(representative.representativeType) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(representative.clazz.classCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(representative.clazz.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 )
            ORDER BY representative.id DESC
            """)
    List<ClassRepresentative> searchRepresentatives(
            @Param("departmentId") Long departmentId,
            @Param("classId") Long classId,
            @Param("active") Boolean active,
            @Param("keyword") String keyword);
}
