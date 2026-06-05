package com.example.userservice.feature.preference.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSettings {
    private Boolean newActivityAlert;
    private Boolean reminderAlert;
    private Integer reminderDaysBefore;
}
