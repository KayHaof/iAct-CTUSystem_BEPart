package com.example.activityservice.feature.activities.service.impl;

import com.example.activityservice.feature.activities.dto.ActivityRequest;
import com.example.activityservice.feature.activities.dto.ActivityResponse;
import com.example.activityservice.feature.activities.mapper.ActivityMapper;
import com.example.activityservice.feature.activities.model.Activities;
import com.example.activityservice.feature.activities.repository.ActivityRepository;
import com.example.activityservice.feature.activitySchedule.mapper.ActivityScheduleMapper;
import com.example.activityservice.feature.benefits.dto.BenefitResponse;
import com.example.activityservice.feature.benefits.mapper.BenefitMapper;
import com.example.activityservice.feature.benefits.model.Benefits;
import com.example.activityservice.feature.benefits.repository.BenefitRepository;
import com.example.activityservice.feature.benefits.service.BenefitValidationService;
import com.example.activityservice.feature.categories.model.Categories;
import com.example.activityservice.feature.organizers.repository.OrganizerRepository;
import com.example.activityservice.feature.registration.repository.RegistrationRepository;
import com.example.activityservice.feature.semesters.repository.SemesterRepository;
import com.example.activityservice.feature.users.repository.UserRepository;
import com.example.activityservice.feature.users.model.Users;
import com.example.activityservice.feature.users.service.LocalUserResolver;
import com.example.activityservice.service.CloudinaryService;
import com.example.activityservice.service.QRCodeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityServiceImplTest {

    @Mock
    private ActivityRepository activityRepository;
    @Mock
    private SemesterRepository semesterRepository;
    @Mock
    private OrganizerRepository organizerRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private LocalUserResolver localUserResolver;
    @Mock
    private RegistrationRepository registrationRepository;
    @Mock
    private BenefitRepository benefitRepository;
    @Mock
    private ActivityMapper activityMapper;
    @Mock
    private ActivityScheduleMapper scheduleMapper;
    @Mock
    private BenefitMapper benefitMapper;
    @Mock
    private BenefitValidationService benefitValidationService;
    @Mock
    private CloudinaryService cloudinaryService;
    @Mock
    private QRCodeService qrCodeService;
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private ActivityServiceImpl activityService;

    @Test
    void createActivityAllowsDraftWithoutStartDate() {
        SecurityContextHolder.clearContext();

        ActivityRequest request = new ActivityRequest();
        request.setTitle("Bản nháp hoạt động");
        request.setStatus(3);

        Activities draft = new Activities();
        draft.setId(1L);
        draft.setTitle(request.getTitle());
        draft.setStatus(3);
        ActivityResponse expectedResponse = new ActivityResponse();

        when(activityMapper.toEntity(request, null)).thenReturn(draft);
        when(activityRepository.save(draft)).thenReturn(draft);
        when(activityMapper.toResponse(draft)).thenReturn(expectedResponse);

        ActivityResponse response = activityService.createActivity(request);

        assertSame(expectedResponse, response);
        verify(semesterRepository, never()).findSemesterByDate(org.mockito.ArgumentMatchers.any());
        verify(activityRepository).save(draft);
    }

    @Test
    void createActivityPersistsValidatedBenefits() {
        SecurityContextHolder.clearContext();

        BenefitResponse requestedBenefit = new BenefitResponse();
        requestedBenefit.setCategoryId(10L);
        requestedBenefit.setPoint(4);
        requestedBenefit.setType(1);

        ActivityRequest request = new ActivityRequest();
        request.setTitle("Bản nháp có quyền lợi");
        request.setStatus(3);
        request.setBenefits(List.of(requestedBenefit));

        Activities draft = new Activities();
        draft.setId(2L);
        draft.setStatus(3);
        Categories category = Categories.builder()
                .id(10L)
                .name("Tiêu chí lá")
                .maxPoint(5)
                .isActive(true)
                .build();
        Benefits savedBenefit = Benefits.builder()
                .activity(draft)
                .category(category)
                .point(4)
                .type(1)
                .build();
        ActivityResponse expectedResponse = new ActivityResponse();

        when(activityMapper.toEntity(request, null)).thenReturn(draft);
        when(activityRepository.save(draft)).thenReturn(draft);
        when(benefitValidationService.validateAndGetCategory(10L, 4, 1)).thenReturn(category);
        when(benefitRepository.saveAll(anyList())).thenReturn(List.of(savedBenefit));
        when(benefitMapper.toResponse(savedBenefit)).thenReturn(requestedBenefit);
        when(activityMapper.toResponse(draft)).thenReturn(expectedResponse);

        ActivityResponse response = activityService.createActivity(request);

        assertSame(expectedResponse, response);
        assertEquals(List.of(requestedBenefit), response.getBenefits());
        verify(benefitRepository).deleteByActivityId(2L);
        verify(benefitRepository).saveAll(anyList());
    }

    @Test
    void createActivityResolvesOrganizerByUserId() {
        SecurityContextHolder.clearContext();

        ActivityRequest request = new ActivityRequest();
        request.setTitle("Bản nháp có người phụ trách");
        request.setStatus(3);
        request.setOrganizerId(13L);

        Users user = new Users();
        user.setId(13L);
        user.setUsername("sv1");
        user.setFullName("Sinh viên Một");
        user.setDepartmentId(2L);

        Activities draft = new Activities();
        draft.setId(3L);
        draft.setStatus(3);
        ActivityResponse expectedResponse = new ActivityResponse();

        when(localUserResolver.resolveById(13L)).thenReturn(user);
        when(organizerRepository.findByUserId(13L)).thenReturn(java.util.Optional.empty());
        when(organizerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(activityMapper.toEntity(eq(request), any())).thenReturn(draft);
        when(activityRepository.save(draft)).thenReturn(draft);
        when(activityMapper.toResponse(draft)).thenReturn(expectedResponse);

        activityService.createActivity(request);

        org.mockito.ArgumentCaptor<com.example.activityservice.feature.organizers.model.Organizers> captor =
                org.mockito.ArgumentCaptor.forClass(
                        com.example.activityservice.feature.organizers.model.Organizers.class);
        verify(organizerRepository).save(captor.capture());
        assertNull(captor.getValue().getId());
        assertEquals(13L, captor.getValue().getUserId());
        assertEquals("Sinh viên Một", captor.getValue().getName());
        assertEquals(2L, captor.getValue().getDepartmentId());
    }
}
