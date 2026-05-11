package com.example.activityservice.feature.organizers.repository;

import com.example.activityservice.feature.organizers.model.Organizers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrganizerRepository extends JpaRepository<Organizers, Long> {
}
