package com.example.activityservice.feature.locations.repository;

import com.example.activityservice.feature.locations.model.Location;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LocationRepository extends JpaRepository<Location, Long> {
    boolean existsByCode(String code);
    boolean existsByCodeAndIdNot(String code, Long id);
    List<Location> findByIsActiveTrue();
    List<Location> findByIsActiveTrueAndIsBookableTrueAndAvailabilityStatus(String availabilityStatus);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Location l where l.id = :id")
    Optional<Location> findByIdForUpdate(@Param("id") Long id);
}
