package com.example.activityservice.feature.proofs.mapper;

import com.example.activityservice.feature.proofs.dto.ProofResponse;
import com.example.activityservice.feature.proofs.dto.ProofSubmissionRequest;
import com.example.activityservice.feature.proofs.model.Proofs;
import com.example.activityservice.feature.registration.model.Registrations;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProofMapper {

    // 1. Entity sang DTO
    @Mapping(target = "registrationId", source = "registration.id")
    @Mapping(target = "activityId", expression = "java(entity.getActivity() != null ? entity.getActivity().getId() : null)")
    @Mapping(target = "activityTitle", expression = "java(entity.getActivity() != null ? entity.getActivity().getTitle() : null)")
    @Mapping(target = "studentId", expression = "java(entity.getStudentId())")
    @Mapping(target = "studentCode", source = "registration.student.studentCode")
    @Mapping(target = "studentName", source = "registration.student.fullName")
    @Mapping(target = "studentAvatarUrl", source = "registration.student.avatarUrl")
    @Mapping(target = "submittedAt", source = "createdAt")
    ProofResponse toResponse(Proofs entity);

    // 2. Map từ Request + Long studentId + Activity => Proofs MỚI
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", expression = "java(0)")
    @Mapping(target = "registration", source = "registration")
    @Mapping(target = "imageUrl", source = "request.imageUrl")
    @Mapping(target = "description", source = "request.description")
    Proofs toNewEntity(ProofSubmissionRequest request, Registrations registration);

    // 3. Update
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "registration", ignore = true)
    @Mapping(target = "status", expression = "java(0)")
    @Mapping(target = "rejectionReason", expression = "java(null)")
    @Mapping(target = "verifiedBy", ignore = true)
    @Mapping(target = "verifiedTime", ignore = true)
    void updateEntityFromRequest(ProofSubmissionRequest request, @MappingTarget Proofs entity);
}
