package com.example.feature.benefits.model;

import com.example.common.entity.Activities;
import com.example.feature.categories.model.Categories;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "benefits")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Benefits {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Activities activity;

    private Integer type; // 1=point, 2=certificate

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Categories category;

    private Integer point;
}

