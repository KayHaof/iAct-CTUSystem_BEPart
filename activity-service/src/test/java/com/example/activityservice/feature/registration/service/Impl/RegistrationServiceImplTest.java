package com.example.activityservice.feature.registration.service.Impl;

import com.example.activityservice.feature.activities.model.Activities;
import com.example.activityservice.feature.activities.repository.ActivityRepository;
import com.example.activityservice.feature.activities.service.ActivityCacheService;
import com.example.activityservice.feature.activitySchedule.repository.ActivityScheduleRepository;
import com.example.activityservice.feature.proofs.repository.ProofRepository;
import com.example.activityservice.feature.registration.kafka.RegistrationKafkaProducer;
import com.example.activityservice.feature.registration.mapper.RegistrationMapper;
import com.example.activityservice.feature.registration.model.Registrations;
import com.example.activityservice.feature.registration.repository.RegistrationRepository;
import com.example.activityservice.feature.users.model.Users;
import com.example.activityservice.feature.users.repository.UserRepository;
import com.example.activityservice.service.ExcelExportService;
import com.example.activityservice.service.QRCodeService;
import com.example.exception.AppException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceImplTest {

    @Mock
    private RegistrationRepository registrationRepository;
    @Mock
    private ActivityRepository activityRepository;
    @Mock
    private ActivityScheduleRepository scheduleRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RegistrationMapper registrationMapper;
    @Mock
    private ExcelExportService excelExportService;
    @Mock
    private ProofRepository proofRepository;
    @Mock
    private QRCodeService qrCodeService;
    @Mock
    private RegistrationKafkaProducer registrationKafkaProducer;
    @Mock
    private ActivityCacheService activityCacheService;
    @Spy
    @InjectMocks
    private RegistrationServiceImpl service;

    @Test
    void registerRejectsFacultyActivityWhenStudentDepartmentDiffers() {
        Users student = new Users();
        student.setId(10L);
        student.setDepartmentId(1L);

        Activities activity = new Activities();
        activity.setId(20L);
        activity.setStatus(1);
        activity.setIsFaculty(true);
        activity.setIsExternal(false);
        activity.setDepartmentId(2L);
        activity.setRegistrationStart(LocalDateTime.now().minusDays(1));
        activity.setRegistrationEnd(LocalDateTime.now().plusDays(1));

        com.example.activityservice.feature.registration.dto.RegistrationRequest request =
                new com.example.activityservice.feature.registration.dto.RegistrationRequest();
        request.setActivityId(20L);

        doReturn(student).when(service).getCurrentStudent();
        when(activityRepository.findByIdForRegistrationUpdate(20L)).thenReturn(Optional.of(activity));

        assertThrows(AppException.class, () -> service.register(request));

        verify(registrationRepository, never()).save(any());
    }

    @Test
    void registerUsesActivityWriteLockAndRejectsWhenFull() {
        Users student = new Users();
        student.setId(10L);
        student.setDepartmentId(1L);

        Activities activity = new Activities();
        activity.setId(20L);
        activity.setStatus(1);
        activity.setIsFaculty(false);
        activity.setIsExternal(false);
        activity.setDepartmentId(1L);
        activity.setMaxParticipants(1);
        activity.setRegistrationStart(LocalDateTime.now().minusDays(1));
        activity.setRegistrationEnd(LocalDateTime.now().plusDays(1));

        com.example.activityservice.feature.registration.dto.RegistrationRequest request =
                new com.example.activityservice.feature.registration.dto.RegistrationRequest();
        request.setActivityId(20L);

        doReturn(student).when(service).getCurrentStudent();
        when(activityRepository.findByIdForRegistrationUpdate(20L)).thenReturn(Optional.of(activity));
        when(registrationRepository.findByStudentIdAndActivityId(10L, 20L)).thenReturn(Optional.empty());
        when(registrationRepository.countByActivityIdAndStatusNot(20L, 2)).thenReturn(1L);

        assertThrows(AppException.class, () -> service.register(request));

        verify(activityRepository).findByIdForRegistrationUpdate(20L);
        verify(registrationRepository, never()).save(any());
    }

    @Test
    void getMyRecordsFiltersByActivitySemesterAssociation() {
        Users student = new Users();
        student.setId(10L);
        doReturn(student).when(service).getCurrentStudent();
        when(registrationRepository.findAll(anySpecification(), any(Sort.class)))
                .thenReturn(List.of());

        service.getMyRecords(1L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Specification<Registrations>> specificationCaptor = ArgumentCaptor.forClass(Specification.class);
        verify(registrationRepository).findAll(specificationCaptor.capture(), any(Sort.class));

        @SuppressWarnings("unchecked")
        Root<Registrations> root = org.mockito.Mockito.mock(Root.class);

        CriteriaQuery<?> query = org.mockito.Mockito.mock(CriteriaQuery.class);
        CriteriaBuilder criteriaBuilder = org.mockito.Mockito.mock(CriteriaBuilder.class);
        Path<?> studentPath = org.mockito.Mockito.mock(Path.class);
        Path<?> studentIdPath = org.mockito.Mockito.mock(Path.class);
        Path<?> activityPath = org.mockito.Mockito.mock(Path.class);
        Path<?> semesterPath = org.mockito.Mockito.mock(Path.class);
        Path<?> semesterIdPath = org.mockito.Mockito.mock(Path.class);

        doReturn(studentPath).when(root).get("student");
        doReturn(studentIdPath).when(studentPath).get("id");
        doReturn(activityPath).when(root).get("activity");
        doReturn(semesterPath).when(activityPath).get("semester");
        doReturn(semesterIdPath).when(semesterPath).get("id");

        specificationCaptor.getValue().toPredicate(root, query, criteriaBuilder);

        verify(activityPath).get("semester");
        verify(semesterPath).get("id");
        verify(criteriaBuilder).equal(semesterIdPath, 1L);
    }

    private static Specification<Registrations> anySpecification() {
        return any();
    }
}
