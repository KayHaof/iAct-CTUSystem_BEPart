package com.example.feature.user_profile.repository;

import com.example.feature.user_profile.model.DepartmentProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepartmentProfileRepository extends JpaRepository<DepartmentProfile, Long> {

    @Query("SELECT d.userId FROM DepartmentProfile d WHERE " +
            "(:keyword IS NULL OR LOWER(d.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Long> searchIdsByKeyword(@Param("keyword") String keyword);

    @Query("SELECT d.userId FROM DepartmentProfile d WHERE " +
            "(:keyword IS NULL OR :keyword = '' OR LOWER(d.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:departmentId IS NULL OR d.department.id = :departmentId)")
    List<Long> searchIdsByCriteria(@Param("keyword") String keyword, @Param("departmentId") Long departmentId);
}
