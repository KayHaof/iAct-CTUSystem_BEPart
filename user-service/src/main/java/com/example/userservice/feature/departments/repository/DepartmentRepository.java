package com.example.userservice.feature.departments.repository;

import com.example.userservice.feature.departments.model.Departments;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface DepartmentRepository extends JpaRepository<Departments, Long>, JpaSpecificationExecutor<Departments> {
}
