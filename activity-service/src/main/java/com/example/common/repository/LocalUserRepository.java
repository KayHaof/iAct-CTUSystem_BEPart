package com.example.common.repository;

import com.example.common.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LocalUserRepository extends JpaRepository<Users, Long> {
    Optional<Users> findByUsername(String username);

}
