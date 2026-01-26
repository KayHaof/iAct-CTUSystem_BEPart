package com.example.common;

import com.example.feature.activities.model.Activities;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "benefits")
@Data
public class Benefits {
    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id")
    private Activities activity;

    private Integer type; // 1=point, 2=certificate

//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "category_id")
//    private Categories category;

    private Integer point;
}

