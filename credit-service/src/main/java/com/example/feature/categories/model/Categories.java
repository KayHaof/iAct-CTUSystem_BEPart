package com.example.feature.categories.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "categories")
@Data
public class Categories {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String code;
    private String name;

    @Column(name = "max_point")
    private Integer maxPoint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Categories parent;

    @OneToMany(mappedBy = "parent")
    private List<Categories> subCategories;
}

