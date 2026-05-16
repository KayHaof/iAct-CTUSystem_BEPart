package com.example.activityservice.feature.semesters.repository;

import com.example.activityservice.feature.semesters.model.Semesters;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SemesterRepository extends JpaRepository<Semesters, Long> {
    Optional<Semesters> findByIsActiveTrue();

    List<Semesters> findByIsActive(Boolean isActive);

    List<Semesters> findByIsLocked(Boolean isLocked);

    List<Semesters> findByAcademicYear(String academicYear);

    List<Semesters> findByIsActiveAndIsLocked(Boolean isActive, Boolean isLocked);

    List<Semesters> findByIsActiveAndAcademicYear(Boolean isActive, String academicYear);

    List<Semesters> findByIsLockedAndAcademicYear(Boolean isLocked, String academicYear);

    List<Semesters> findByIsActiveAndIsLockedAndAcademicYear(Boolean isActive, Boolean isLocked, String academicYear);

    boolean existsByNameAndAcademicYear(String name, String academicYear);

    boolean existsByNameAndAcademicYearAndIdNot(String name, String academicYear, Long id);

    @Modifying
    @Query("UPDATE Semesters s SET s.isActive = false WHERE s.isActive = true")
    void deactivateAllSemesters();

    @Query("SELECT s FROM Semesters s WHERE :date BETWEEN s.startDate AND s.endDate")
    Optional<Semesters> findSemesterByDate(@Param("date") LocalDate date);
}
