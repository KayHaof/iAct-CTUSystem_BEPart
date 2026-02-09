package com.example.feature.activities.event;

import com.example.feature.activities.model.Activities;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ActivityCreatedEvent {
    private Activities activity;
    private String title;
    private String message;
    private Integer type;
}