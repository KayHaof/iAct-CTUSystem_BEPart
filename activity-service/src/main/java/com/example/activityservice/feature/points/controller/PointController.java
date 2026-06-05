package com.example.activityservice.feature.points.controller;

import com.example.activityservice.feature.points.dto.CategoryPointResponse;
import com.example.activityservice.feature.points.dto.PointDetailsResponse;
import com.example.activityservice.feature.points.dto.PointSummaryResponse;
import com.example.activityservice.feature.points.service.PointService;
import com.example.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/student-points")
@RequiredArgsConstructor
public class PointController {

    private final PointService pointService;

    /**
     * UC08: Lay tong diem ren luyen cua sinh vien
     */
    @GetMapping("/summary")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<PointSummaryResponse> getStudentPointSummary(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Long semesterId) {
        Long studentId = extractStudentId(jwt);
        return ApiResponse.success(pointService.getStudentPointSummary(studentId, semesterId));
    }

    /**
     * UC08: Lay chi tiet diem ren luyen theo dieu muc
     */
    @GetMapping("/details")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<PointDetailsResponse> getStudentPointDetails(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Long semesterId) {
        Long studentId = extractStudentId(jwt);
        return ApiResponse.success(pointService.getStudentPointDetails(studentId, semesterId));
    }

    /**
     * UC08: Lay cau truc dieu muc diem ren luyen
     */
    @GetMapping("/categories")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<CategoryPointResponse>> getCategoriesWithPoints(
            @RequestParam(required = false) Long semesterId) {
        return ApiResponse.success(pointService.getCategoriesWithPoints(semesterId));
    }

    private Long extractStudentId(Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        return pointService.getStudentIdByUsername(username);
    }
}
