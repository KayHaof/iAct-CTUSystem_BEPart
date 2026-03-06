package com.example.feature.semesters.repository;

import com.example.feature.semesters.model.Semesters;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SemesterRepository extends JpaRepository<Semesters, Long> {

    // Lấy học kỳ đang active hiện tại
    Optional<Semesters> findByIsActiveTrue();

    // Query tắt toàn bộ học kỳ đang active (Dùng khi set học kỳ mới làm active)
    @Modifying
    @Query("UPDATE Semesters s SET s.isActive = false WHERE s.isActive = true")
    void deactivateAllSemesters();
}
