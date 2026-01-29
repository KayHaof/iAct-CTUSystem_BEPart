package com.example.common.repository;

import com.example.common.entity.Semesters;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LocalSemesterRepository extends JpaRepository<Semesters, Long> {
}
