package com.example.activityservice.feature.complaints.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ComplaintRequest {
    @NotNull
    private Long registrationId;

    @NotBlank
    @Size(min = 10, max = 2000)
    private String detail;

    @Size(max = 500)
    private String evidenceUrl;
}
