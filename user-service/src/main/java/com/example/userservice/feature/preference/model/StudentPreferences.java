package com.example.userservice.feature.preference.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "student_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentPreferences {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "category_ratings", columnDefinition = "JSON")
    private String categoryRatings;  // JSON: {"1": 5, "2": 3}

    @Column(name = "category_enabled", columnDefinition = "JSON")
    private String categoryEnabled;  // JSON: {"1": true, "2": false}

    @Column(name = "preferred_category_ids", columnDefinition = "JSON")
    private String preferredCategoryIds;  // JSON: [1, 2, 3]

    @Column(name = "notification_settings", columnDefinition = "JSON")
    private String notificationSettings;  // JSON

    @Column(name = "excluded_categories", columnDefinition = "JSON")
    private String excludedCategories;  // JSON: ["4", "5"]

    @Column(name = "ai_recommendation_enabled")
    @Builder.Default
    private Boolean aiRecommendationEnabled = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
