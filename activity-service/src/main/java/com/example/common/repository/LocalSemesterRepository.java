package com.example.common.repository;

import com.example.common.entity.Semesters;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface LocalSemesterRepository extends JpaRepository<Semesters, Long> {
    @Query("SELECT s FROM Semesters s WHERE :targetDate BETWEEN s.startDate AND s.endDate")
    Optional<Semesters> findSemesterByDate(@Param("targetDate") LocalDate targetDate);
}
