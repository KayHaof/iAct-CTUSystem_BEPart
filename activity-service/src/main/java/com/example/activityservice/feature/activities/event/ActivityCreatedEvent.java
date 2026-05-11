package com.example.activityservice.feature.activities.event;

import com.example.activityservice.feature.activities.model.Activities;
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