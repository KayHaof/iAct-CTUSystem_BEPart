package com.example.activityservice.feature.complaints.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResolveComplaintRequest {
    @NotBlank(message = "Noi dung phan hoi khong duoc de trong")
    private String response;
}
