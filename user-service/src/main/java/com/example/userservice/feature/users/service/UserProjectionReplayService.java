package com.example.userservice.feature.users.service;

import com.example.userservice.feature.user_profile.dto.ProfileDto;
import com.example.userservice.feature.user_profile.service.UserProfileService;
import com.example.userservice.feature.users.model.Users;
import com.example.userservice.feature.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserProjectionReplayService {

    private static final int PAGE_SIZE = 100;

    private final UserRepository userRepository;
    private final UserProfileService userProfileService;
    private final UserProjectionPublisher projectionPublisher;

    @Transactional(readOnly = true)
    public long replayAll() {
        int pageNumber = 0;
        long scheduledEvents = 0;
        Page<Users> page;
        do {
            page = userRepository.findAll(PageRequest.of(
                    pageNumber++, PAGE_SIZE, Sort.by(Sort.Direction.ASC, "id")));
            List<Users> users = page.getContent();
            Map<Long, ProfileDto> profiles = userProfileService.getProfilesBatch(
                    users.stream().map(Users::getId).toList());
            for (Users user : users) {
                projectionPublisher.publish(user, profiles.get(user.getId()));
                scheduledEvents++;
            }
        } while (page.hasNext());
        return scheduledEvents;
    }
}
