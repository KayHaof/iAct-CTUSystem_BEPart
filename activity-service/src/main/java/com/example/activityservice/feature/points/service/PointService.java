package com.example.activityservice.feature.points.service;

import com.example.activityservice.feature.points.dto.CategoryPointResponse;
import com.example.activityservice.feature.points.dto.PointDetailsResponse;
import com.example.activityservice.feature.points.dto.PointSummaryResponse;

import java.util.List;

public interface PointService {
    
    PointSummaryResponse getStudentPointSummary(Long studentId, Long semesterId);
    
    PointDetailsResponse getStudentPointDetails(Long studentId, Long semesterId);
    
    List<CategoryPointResponse> getCategoriesWithPoints(Long semesterId);
    
    Long getStudentIdByUsername(String username);
}
