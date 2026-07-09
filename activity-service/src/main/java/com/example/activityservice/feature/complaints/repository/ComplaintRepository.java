package com.example.activityservice.feature.complaints.repository;

import com.example.activityservice.feature.complaints.model.Complaints;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ComplaintRepository extends JpaRepository<Complaints, Long> {
    @EntityGraph(attributePaths = {"registration", "registration.activity"})
    Optional<Complaints> findByRegistrationId(Long registrationId);

    @EntityGraph(attributePaths = {"registration", "registration.activity"})
    List<Complaints> findByRegistrationIdIn(Collection<Long> registrationIds);
}
