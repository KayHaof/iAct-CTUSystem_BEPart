package com.example.userservice.feature.major.repository;

import com.example.userservice.feature.major.model.Major;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MajorRepository extends JpaRepository<Major, Long>, JpaSpecificationExecutor<Major> {
    @Override
    @EntityGraph(attributePaths = "department")
    List<Major> findAll(Sort sort);

    @EntityGraph(attributePaths = "department")
    List<Major> findByDepartmentId(Long departmentId);

    @EntityGraph(attributePaths = "department")
    List<Major> findByDepartmentIdAndIsActive(Long departmentId, Boolean isActive);

    @EntityGraph(attributePaths = "department")
    List<Major> findByIsActive(Boolean isActive);

    @Override
    @EntityGraph(attributePaths = "department")
    Page<Major> findAll(Specification<Major> spec, Pageable pageable);

    boolean existsByDepartmentId(Long departmentId);

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);

    boolean existsByNameIgnoreCaseAndDepartmentId(String name, Long departmentId);

    boolean existsByNameIgnoreCaseAndDepartmentIdAndIdNot(String name, Long departmentId, Long id);
}
