package com.example.userservice.feature.departments.repository;

import com.example.userservice.feature.departments.model.Departments;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepartmentRepository extends JpaRepository<Departments, Long>, JpaSpecificationExecutor<Departments> {
    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);

    List<Departments> findByIsActive(Boolean isActive, Sort sort);
}
