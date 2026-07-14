package com.example.activityservice.feature.locations.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "locations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 50, unique = true)
    private String code;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 255)
    private String address;

    @Column(length = 100)
    private String building;

    @Column(name = "floor_label", length = 50)
    private String floor;

    @Column(length = 50)
    private String room;

    private Integer capacity;

    @Column(name = "manager_department_id")
    private Long managerDepartmentId;

    @Column(name = "manager_user_id")
    private Long managerUserId;

    @Column(name = "contact_name", length = 100)
    private String contactName;

    @Column(name = "contact_phone", length = 30)
    private String contactPhone;

    @Column(name = "is_bookable")
    @Builder.Default
    private Boolean isBookable = true;

    @Column(name = "availability_status", length = 30)
    @Builder.Default
    private String availabilityStatus = "AVAILABLE";

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "unavailable_reason", columnDefinition = "TEXT")
    private String unavailableReason;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (isBookable == null) {
            isBookable = true;
        }
        if (isActive == null) {
            isActive = true;
        }
        if (availabilityStatus == null || availabilityStatus.isBlank()) {
            availabilityStatus = "AVAILABLE";
        }
    }
}
