package com.example.feature.users.repository;

import com.example.feature.users.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

@Repository
public interface UserRepository extends JpaRepository<Users, Long>, JpaSpecificationExecutor<Users> {
    Optional<Users> findByUsername(String username);
    Optional<Users> findByEmail(String email);
    Optional<Users> findByKeycloakId(String keycloakID);
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);
    Boolean existsByKeycloakId(String keycloakId);

    @Query("SELECT u.username FROM Users u WHERE u.username IN :usernames")
    Set<String> findExistingUsernames(@Param("usernames") Collection<String> usernames);

    @Query("SELECT u.email FROM Users u WHERE u.email IN :emails")
    Set<String> findExistingEmails(@Param("emails") Collection<String> emails);
}