package com.example.activityservice.feature.registration.service.Impl;

import com.example.activityservice.feature.activities.repository.ActivityRepository;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
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
    @Spy
    @InjectMocks
    private RegistrationServiceImpl service;

    @Test
    void getMyRecordsFiltersByActivitySemesterAssociation() {
        Users student = new Users();
        student.setId(10L);
        doReturn(student).when(service).getCurrentStudent();
        when(registrationRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of());

        service.getMyRecords(1L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Specification<Registrations>> specificationCaptor =
                ArgumentCaptor.forClass(Specification.class);
        verify(registrationRepository).findAll(specificationCaptor.capture(), any(Sort.class));

        @SuppressWarnings("unchecked")
        Root<Registrations> root = org.mockito.Mockito.mock(Root.class);
        @SuppressWarnings("unchecked")
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
}
