package com.example.feature.classes.repository;

import com.example.feature.classes.model.Clazzes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ClassRepository extends JpaRepository<Clazzes, Long> {
    @Query("SELECT c.id FROM Clazzes c WHERE c.major.department.id = :departmentId")
    List<Long> findClassIdsByDepartmentId(@Param("departmentId") Long departmentId);

    @Query("SELECT c FROM Clazzes c WHERE c.major.id = :majorId AND (:academicYear IS NULL OR c.academicYear = :academicYear)")
    List<Clazzes> findClassesByMajorAndYear(@Param("majorId") Long majorId, @Param("academicYear") Integer academicYear);

    boolean existsByClassCode(String classCode);
}
