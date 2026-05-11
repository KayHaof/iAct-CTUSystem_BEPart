package com.example.activityservice.feature.users.repository;

import com.example.activityservice.feature.users.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<Users, Long> {
    Optional<Users> findByUsername(String username);

    @Query("SELECT u.id FROM Users u WHERE LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(u.studentCode) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Long> searchIdsByKeyword(@Param("keyword") String keyword);
}
