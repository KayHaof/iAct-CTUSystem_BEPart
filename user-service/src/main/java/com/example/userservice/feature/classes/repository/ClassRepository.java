package com.example.userservice.feature.classes.repository;

import com.example.userservice.feature.classes.model.Clazzes;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface ClassRepository extends JpaRepository<Clazzes, Long>, JpaSpecificationExecutor<Clazzes> {

    @Override
    @EntityGraph(attributePaths = {"major", "major.department"})
    List<Clazzes> findAll(Sort sort);

    @Override
    @EntityGraph(attributePaths = {"major", "major.department"})
    Page<Clazzes> findAll(Specification<Clazzes> spec, Pageable pageable);

    @Query("SELECT c.id FROM Clazzes c WHERE c.major.department.id = :departmentId")
    List<Long> findClassIdsByDepartmentId(Long departmentId);

    @EntityGraph(attributePaths = {"major", "major.department"})
    List<Clazzes> findByMajorId(Long majorId);

    @EntityGraph(attributePaths = {"major", "major.department"})
    List<Clazzes> findByMajorIdAndAcademicYear(Long majorId, String academicYear);

    @Query("SELECT c FROM Clazzes c JOIN FETCH c.major m JOIN FETCH m.department d WHERE c.major.id = :majorId AND (:academicYear IS NULL OR c.academicYear = :academicYear)")
    List<Clazzes> findClassesByMajorAndYear(Long majorId, String academicYear);

    boolean existsByClassCode(String classCode);

    boolean existsByClassCodeAndIdNot(String classCode, Long id);

    boolean existsByMajorId(Long majorId);

    List<Clazzes> findByClassCodeIn(Set<String> classCodes);
}
