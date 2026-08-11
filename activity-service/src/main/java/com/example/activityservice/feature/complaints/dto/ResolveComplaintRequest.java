package com.example.activityservice.feature.complaints.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResolveComplaintRequest {
    @NotBlank(message = "Nội dung phản hồi không được để trống")
    private String response;
}
