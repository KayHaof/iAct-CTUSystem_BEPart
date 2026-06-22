package com.example.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSnapshotEvent {
    private String eventId;
    private Integer eventVersion;
    private Long userId;
    private String username;
    private String email;
    private String fullName;
    private String studentCode;
    private String avatarUrl;
    private Long departmentId;
    private String occurredAt;
}
