package com.example.feature.users.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserDisabledEvent {
    private Long userId;
    private String title;
    private String message;
    private int type;
}