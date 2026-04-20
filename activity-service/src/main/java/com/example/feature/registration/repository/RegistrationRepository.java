package com.example.feature.registration.repository;

import com.example.feature.registration.model.Registrations;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RegistrationRepository extends JpaRepository<Registrations, Long>, JpaSpecificationExecutor<Registrations> {

    boolean existsByStudentIdAndActivityId(Long studentId, Long activityId);

    Optional<Registrations> findByStudentIdAndActivityId(Long studentId, Long activityId);

    long countByActivityIdAndStatusNot(Long activityId, Integer status);

    List<Registrations> findAllByActivityId(Long activityId);
}