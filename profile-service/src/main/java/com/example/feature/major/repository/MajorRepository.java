package com.example.feature.major.repository;

import com.example.feature.major.model.Major;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MajorRepository extends JpaRepository<Major, Long> {
    List<Major> findByDepartmentId(Long departmentId);
    boolean existsByNameAndDepartmentId(String name, Long departmentId);
}
