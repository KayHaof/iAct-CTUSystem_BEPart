package com.example.userservice.feature.classes.service;

import com.example.userservice.feature.classes.dto.RepresentativeActivityPermissionResponse;
import com.example.userservice.feature.classes.dto.ClassRepresentativeRequest;

import java.util.List;

public interface ClassRepresentativeService {
    RepresentativeActivityPermissionResponse getCurrentStudentActivityPermission();
    List<RepresentativeActivityPermissionResponse> getRepresentatives(
            Long departmentId,
            Long classId,
            Boolean active,
            String keyword);
    RepresentativeActivityPermissionResponse createRepresentative(ClassRepresentativeRequest request);
    RepresentativeActivityPermissionResponse deactivateRepresentative(Long id);
}
