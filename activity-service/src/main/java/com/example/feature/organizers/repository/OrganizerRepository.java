package com.example.feature.organizers.repository;

import com.example.feature.organizers.model.Organizers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrganizerRepository extends JpaRepository<Organizers, Long> {
}
