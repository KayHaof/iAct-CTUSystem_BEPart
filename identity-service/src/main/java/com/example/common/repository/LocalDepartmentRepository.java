package com.example.common.repository;

import com.example.common.entity.Departments;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LocalDepartmentRepository extends JpaRepository<Departments, Long> {
}
