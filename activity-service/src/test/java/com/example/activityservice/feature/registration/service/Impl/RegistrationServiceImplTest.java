package com.example.activityservice.feature.registration.service.Impl;

import com.example.activityservice.feature.activities.model.Activities;
import com.example.activityservice.feature.activities.repository.ActivityRepository;
import com.example.activityservice.feature.activities.service.ActivityCacheService;
import com.example.activityservice.feature.activitySchedule.model.ActivitySchedule;
import com.example.activityservice.feature.activitySchedule.repository.ActivityScheduleRepository;
import com.example.activityservice.feature.attendances.repository.AttendanceRepository;
import com.example.activityservice.feature.attendances.repository.FaceCheckInAttemptRepository;
import com.example.activityservice.feature.attendances.service.AttendanceService;
import com.example.activityservice.feature.face_embedding.service.StudentFaceEmbeddingProjectionService;
import com.example.activityservice.feature.proofs.repository.ProofRepository;
import com.example.activityservice.feature.registration.kafka.RegistrationKafkaProducer;
import com.example.activityservice.feature.registration.dto.RegistrationRequest;
import com.example.activityservice.feature.registration.mapper.RegistrationMapper;
import com.example.activityservice.feature.registration.model.Registrations;
import com.example.activityservice.feature.registration.repository.RegistrationRepository;
import com.example.activityservice.feature.users.model.Users;
import com.example.activityservice.feature.users.repository.UserRepository;
import com.example.activityservice.service.ExcelExportService;
import com.example.activityservice.feature.activities.service.impl.ActivityAccessSupport;
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
    private StudentFaceEmbeddingProjectionService faceEmbeddingProjectionService;
    @Mock
    private FaceCheckInAttemptRepository faceCheckInAttemptRepository;
    @Mock
    private AttendanceRepository attendanceRepository;
    @Mock
    private AttendanceService attendanceService;
    @Mock
    private ActivityAccessSupport accessSupport;
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
        when(userRepository.findByIdForRegistrationUpdate(10L)).thenReturn(Optional.of(student));
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
        when(userRepository.findByIdForRegistrationUpdate(10L)).thenReturn(Optional.of(student));
        when(activityRepository.findByIdForRegistrationUpdate(20L)).thenReturn(Optional.of(activity));
        when(registrationRepository.findByStudentIdAndActivityId(10L, 20L)).thenReturn(Optional.empty());
        when(registrationRepository.countByActivityIdAndStatusNot(20L, 2)).thenReturn(1L);

        assertThrows(AppException.class, () -> service.register(request));

        verify(activityRepository).findByIdForRegistrationUpdate(20L);
        verify(registrationRepository, never()).save(any());
    }

    @Test
    void registerRejectsOverlappingScheduleFromAnotherActivity() {
        Users student = student(10L);
        Activities targetActivity = openActivity(20L, "Hoạt động mới");
        ActivitySchedule selectedSchedule = schedule(101L, targetActivity, "B1", 10, 0, 11, 0);

        Activities existingActivity = openActivity(30L, "Hoạt động đã đăng ký");
        ActivitySchedule existingSchedule = schedule(301L, existingActivity, "B2", 10, 30, 11, 30);
        Registrations existingRegistration = registration(student, existingActivity, existingSchedule);

        RegistrationRequest request = new RegistrationRequest();
        request.setActivityId(targetActivity.getId());
        request.setScheduleIds(List.of(selectedSchedule.getId()));

        stubRegistrationContext(student, targetActivity, selectedSchedule, existingRegistration);

        assertThrows(AppException.class, () -> service.register(request));
        verify(registrationRepository, never()).save(any());
        verify(faceEmbeddingProjectionService, never()).ensureActiveForRegistration(any());
    }

    @Test
    void registerAllowsSchedulesThatOnlyTouchAtBoundary() {
        Users student = student(10L);
        Activities targetActivity = openActivity(20L, "Hoạt động mới");
        ActivitySchedule selectedSchedule = schedule(101L, targetActivity, "B1", 10, 0, 11, 0);

        Activities existingActivity = openActivity(30L, "Hoạt động đã đăng ký");
        ActivitySchedule existingSchedule = schedule(301L, existingActivity, "B2", 11, 0, 12, 0);
        Registrations existingRegistration = registration(student, existingActivity, existingSchedule);

        RegistrationRequest request = new RegistrationRequest();
        request.setActivityId(targetActivity.getId());
        request.setScheduleIds(List.of(selectedSchedule.getId()));

        stubRegistrationContext(student, targetActivity, selectedSchedule, existingRegistration);

        Registrations newRegistration = registration(student, targetActivity, selectedSchedule);
        newRegistration.setId(501L);
        when(registrationMapper.toNewEntity(student, targetActivity, List.of(selectedSchedule)))
                .thenReturn(newRegistration);
        when(registrationRepository.save(newRegistration)).thenReturn(newRegistration);
        when(attendanceRepository.findAllByRegistrationId(501L)).thenReturn(List.of());
        when(registrationMapper.toResponse(newRegistration))
                .thenReturn(new com.example.activityservice.feature.registration.dto.RegistrationResponse());

        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> service.register(request));
        verify(registrationRepository).save(newRegistration);
    }

    @Test
    void updateSessionsRejectsOverlappingScheduleFromAnotherActivity() {
        Users student = student(10L);
        Activities targetActivity = openActivity(20L, "Hoạt động hiện tại");
        ActivitySchedule selectedSchedule = schedule(101L, targetActivity, "B1", 10, 0, 11, 0);
        Registrations currentRegistration = registration(student, targetActivity, selectedSchedule);
        currentRegistration.setId(201L);

        Activities existingActivity = openActivity(30L, "Hoạt động đã đăng ký");
        ActivitySchedule existingSchedule = schedule(301L, existingActivity, "B2", 10, 30, 11, 30);
        Registrations existingRegistration = registration(student, existingActivity, existingSchedule);

        doReturn(student).when(service).getCurrentStudent();
        when(userRepository.findByIdForRegistrationUpdate(10L)).thenReturn(Optional.of(student));
        when(registrationRepository.findById(201L)).thenReturn(Optional.of(currentRegistration));
        when(scheduleRepository.findByActivityId(targetActivity.getId())).thenReturn(List.of(selectedSchedule));
        when(attendanceRepository.findAllByRegistrationId(201L)).thenReturn(List.of());
        when(registrationRepository.findActiveRegistrationsWithSchedulesByStudentId(
                10L, Registrations.STATUS_CANCELLED)).thenReturn(List.of(existingRegistration));

        assertThrows(
                AppException.class,
                () -> service.updateSessions(201L, List.of(selectedSchedule.getId())));
        verify(registrationRepository, never()).save(any());
    }

    @Test
    void registerRejectsBeforeRegistrationStart() {
        Users student = student(10L);
        Activities activity = openActivity(20L, "Hoạt động chưa mở đăng ký");
        activity.setRegistrationStart(LocalDateTime.now().plusMinutes(1));
        activity.setRegistrationEnd(LocalDateTime.now().plusHours(1));

        RegistrationRequest request = new RegistrationRequest();
        request.setActivityId(activity.getId());

        doReturn(student).when(service).getCurrentStudent();
        when(userRepository.findByIdForRegistrationUpdate(student.getId())).thenReturn(Optional.of(student));
        when(activityRepository.findByIdForRegistrationUpdate(activity.getId())).thenReturn(Optional.of(activity));

        assertThrows(AppException.class, () -> service.register(request));
        verify(registrationRepository, never()).save(any());
    }

    @Test
    void registerRejectsWhenRegistrationWindowIsMissing() {
        Users student = student(10L);
        Activities activity = openActivity(20L, "Hoạt động thiếu thời gian đăng ký");
        activity.setRegistrationStart(null);
        activity.setRegistrationEnd(null);

        RegistrationRequest request = new RegistrationRequest();
        request.setActivityId(activity.getId());

        doReturn(student).when(service).getCurrentStudent();
        when(userRepository.findByIdForRegistrationUpdate(student.getId())).thenReturn(Optional.of(student));
        when(activityRepository.findByIdForRegistrationUpdate(activity.getId())).thenReturn(Optional.of(activity));

        assertThrows(AppException.class, () -> service.register(request));
        verify(registrationRepository, never()).save(any());
    }

    @Test
    void registerRejectsAfterRegistrationEnd() {
        Users student = student(10L);
        Activities activity = openActivity(20L, "Hoạt động đã đóng đăng ký");
        activity.setRegistrationStart(LocalDateTime.now().minusHours(2));
        activity.setRegistrationEnd(LocalDateTime.now().minusMinutes(1));

        RegistrationRequest request = new RegistrationRequest();
        request.setActivityId(activity.getId());

        doReturn(student).when(service).getCurrentStudent();
        when(userRepository.findByIdForRegistrationUpdate(student.getId())).thenReturn(Optional.of(student));
        when(activityRepository.findByIdForRegistrationUpdate(activity.getId())).thenReturn(Optional.of(activity));

        assertThrows(AppException.class, () -> service.register(request));
        verify(registrationRepository, never()).save(any());
    }

    @Test
    void cancelRejectsWhenActivityHasStarted() {
        Users student = student(10L);
        Activities activity = openActivity(20L, "Hoạt động đang diễn ra");
        activity.setStartDate(LocalDateTime.now().minusMinutes(1));
        activity.setEndDate(LocalDateTime.now().plusHours(1));
        Registrations registration = registration(student, activity,
                schedule(101L, activity, "B1", 10, 0, 11, 0));

        doReturn(student).when(service).getCurrentStudent();
        when(userRepository.findByIdForRegistrationUpdate(student.getId())).thenReturn(Optional.of(student));
        when(registrationRepository.findByStudentIdAndActivityId(student.getId(), activity.getId()))
                .thenReturn(Optional.of(registration));

        assertThrows(AppException.class, () -> service.cancelByActivityId(activity.getId(), "Không tham gia nữa"));
        verify(registrationRepository, never()).save(any());
    }

    @Test
    void cancelRejectsWhenActivityHasEnded() {
        Users student = student(10L);
        Activities activity = openActivity(20L, "Hoạt động đã kết thúc");
        activity.setStartDate(LocalDateTime.now().minusHours(2));
        activity.setEndDate(LocalDateTime.now().minusMinutes(1));
        Registrations registration = registration(student, activity,
                schedule(101L, activity, "B1", 10, 0, 11, 0));

        doReturn(student).when(service).getCurrentStudent();
        when(userRepository.findByIdForRegistrationUpdate(student.getId())).thenReturn(Optional.of(student));
        when(registrationRepository.findByStudentIdAndActivityId(student.getId(), activity.getId()))
                .thenReturn(Optional.of(registration));

        assertThrows(AppException.class, () -> service.cancelByActivityId(activity.getId(), "Không tham gia nữa"));
        verify(registrationRepository, never()).save(any());
    }

    @Test
    void cancelAllowsBeforeActivityStart() {
        Users student = student(10L);
        Activities activity = openActivity(20L, "Hoạt động sắp diễn ra");
        activity.setStartDate(LocalDateTime.now().plusHours(1));
        activity.setEndDate(LocalDateTime.now().plusHours(2));
        Registrations registration = registration(student, activity,
                schedule(101L, activity, "B1", 10, 0, 11, 0));
        when(registrationRepository.findByStudentIdAndActivityId(student.getId(), activity.getId()))
                .thenReturn(Optional.of(registration));
        when(userRepository.findByIdForRegistrationUpdate(student.getId())).thenReturn(Optional.of(student));
        when(registrationRepository.save(registration)).thenReturn(registration);
        when(registrationMapper.toResponse(registration))
                .thenReturn(new com.example.activityservice.feature.registration.dto.RegistrationResponse());

        doReturn(student).when(service).getCurrentStudent();

        org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                () -> service.cancelByActivityId(activity.getId(), "Không tham gia nữa"));
        verify(registrationRepository).save(registration);
    }

    @Test
    void cancelByRegistrationIdRejectsAnotherStudentRegistration() {
        Users currentStudent = student(10L);
        Users otherStudent = student(20L);
        Activities activity = openActivity(20L, "Hoạt động sắp diễn ra");
        activity.setStartDate(LocalDateTime.now().plusHours(1));
        activity.setEndDate(LocalDateTime.now().plusHours(2));
        Registrations registration = registration(otherStudent, activity,
                schedule(101L, activity, "B1", 10, 0, 11, 0));

        doReturn(currentStudent).when(service).getCurrentStudent();
        when(userRepository.findByIdForRegistrationUpdate(currentStudent.getId()))
                .thenReturn(Optional.of(currentStudent));
        when(registrationRepository.findById(501L)).thenReturn(Optional.of(registration));

        assertThrows(AppException.class, () -> service.cancel(501L, "Không tham gia nữa"));
        verify(registrationRepository, never()).save(any());
    }

    private Users student(Long id) {
        Users student = new Users();
        student.setId(id);
        student.setDepartmentId(1L);
        return student;
    }

    private Activities openActivity(Long id, String title) {
        Activities activity = new Activities();
        activity.setId(id);
        activity.setTitle(title);
        activity.setStatus(1);
        activity.setIsFaculty(false);
        activity.setIsExternal(false);
        activity.setDepartmentId(1L);
        activity.setMaxParticipants(100);
        activity.setRegistrationStart(LocalDateTime.now().minusDays(1));
        activity.setRegistrationEnd(LocalDateTime.now().plusDays(1));
        return activity;
    }

    private ActivitySchedule schedule(
            Long id,
            Activities activity,
            String title,
            int startHour,
            int startMinute,
            int endHour,
            int endMinute) {
        ActivitySchedule schedule = new ActivitySchedule();
        schedule.setId(id);
        schedule.setActivity(activity);
        schedule.setTitle(title);
        LocalDateTime date = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        schedule.setStartTime(date.withHour(startHour).withMinute(startMinute));
        schedule.setEndTime(date.withHour(endHour).withMinute(endMinute));
        return schedule;
    }

    private Registrations registration(Users student, Activities activity, ActivitySchedule schedule) {
        Registrations registration = new Registrations();
        registration.setStudent(student);
        registration.setActivity(activity);
        registration.setStatus(Registrations.STATUS_REGISTERED);
        registration.setRegisteredSchedules(List.of(schedule));
        return registration;
    }

    private void stubRegistrationContext(
            Users student,
            Activities targetActivity,
            ActivitySchedule selectedSchedule,
            Registrations existingRegistration) {
        doReturn(student).when(service).getCurrentStudent();
        when(userRepository.findByIdForRegistrationUpdate(student.getId())).thenReturn(Optional.of(student));
        when(activityRepository.findByIdForRegistrationUpdate(targetActivity.getId()))
                .thenReturn(Optional.of(targetActivity));
        when(registrationRepository.findByStudentIdAndActivityId(student.getId(), targetActivity.getId()))
                .thenReturn(Optional.empty());
        when(registrationRepository.countByActivityIdAndStatusNot(targetActivity.getId(), 2)).thenReturn(0L);
        when(scheduleRepository.findByActivityId(targetActivity.getId())).thenReturn(List.of(selectedSchedule));
        when(registrationRepository.findActiveRegistrationsWithSchedulesByStudentId(
                student.getId(), Registrations.STATUS_CANCELLED)).thenReturn(List.of(existingRegistration));
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
