package com.example.activityservice.feature.organizers.repository;

import com.example.activityservice.feature.organizers.model.Organizers;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizerRepository extends JpaRepository<Organizers, Long> {
    Optional<Organizers> findByUserId(Long userId);
}
