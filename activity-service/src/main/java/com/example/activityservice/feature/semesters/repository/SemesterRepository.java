package com.example.activityservice.feature.semesters.repository;

import com.example.activityservice.feature.semesters.model.Semesters;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface SemesterRepository extends JpaRepository<Semesters, Long> {
    Optional<Semesters> findByIsActiveTrue();

    @Modifying
    @Query("UPDATE Semesters s SET s.isActive = false WHERE s.isActive = true")
    void deactivateAllSemesters();

    @Query("SELECT s FROM Semesters s WHERE :date BETWEEN s.startDate AND s.endDate")
    Optional<Semesters> findSemesterByDate(@Param("date") LocalDate date);
}
