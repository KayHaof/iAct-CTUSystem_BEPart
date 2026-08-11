package com.example.activityservice.feature.users.repository;

import com.example.activityservice.feature.users.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<Users, Long> {
    Optional<Users> findByUsername(String username);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM Users u WHERE u.id = :id")
    Optional<Users> findByIdForRegistrationUpdate(@Param("id") Long id);

    long countByRoleType(Integer roleType);

    @Query("SELECT u.id FROM Users u WHERE LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(u.studentCode) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Long> searchIdsByKeyword(@Param("keyword") String keyword);

    @Query("SELECT u.id FROM Users u WHERE u.departmentId = :departmentId AND u.roleType = 1 AND u.status = 1")
    List<Long> findActiveStudentIdsByDepartmentId(@Param("departmentId") Long departmentId);

    @Query("SELECT u.id FROM Users u WHERE u.departmentId = :departmentId AND u.roleType = 2 AND u.status = 1")
    List<Long> findActiveDepartmentUserIdsByDepartmentId(@Param("departmentId") Long departmentId);

    @Query("SELECT u.id FROM Users u WHERE u.roleType = 3 AND u.status = 1")
    List<Long> findActiveAdminUserIds();

    @Query("SELECT u.id FROM Users u WHERE u.roleType = 1 AND u.status = 1")
    List<Long> findActiveStudentIds();

}
