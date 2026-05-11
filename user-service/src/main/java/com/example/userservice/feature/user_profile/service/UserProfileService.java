package com.example.userservice.feature.user_profile.service;

import com.example.userservice.feature.user_profile.dto.CreateProfileDto;
import com.example.userservice.feature.user_profile.dto.ProfileDto;
import com.example.userservice.feature.user_profile.dto.UserUpdateRequest;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface UserProfileService {

    void createProfile(CreateProfileDto dto);

    ProfileDto getProfileByUserId(Long userId);

    Map<Long, ProfileDto> getProfilesBatch(List<Long> userIds);

    Set<Long> searchUserIds(String keyword, Long departmentId, Integer roleType, Long classId);

    void updateUserProfile(Long userId, UserUpdateRequest request);

    void createProfilesBatch(List<CreateProfileDto> profiles);

    Set<String> checkExistingStudentCodes(Set<String> studentCodes);
}