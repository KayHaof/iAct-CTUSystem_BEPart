package com.example.feature.proofs.mapper;

import com.example.feature.activities.model.Activities;
import com.example.feature.proofs.dto.ProofResponse;
import com.example.feature.proofs.dto.ProofSubmissionRequest;
import com.example.feature.proofs.model.Proofs;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProofMapper {

    // 1. Entity sang DTO
    @Mapping(target = "activityId", source = "activity.id")
    ProofResponse toResponse(Proofs entity);

    // 2. Map từ Request + Long studentId + Activity => Proofs MỚI
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", expression = "java(0)")
    @Mapping(target = "studentId", source = "studentId")
    @Mapping(target = "activity", source = "activity")
    @Mapping(target = "imageUrl", source = "request.imageUrl")
    @Mapping(target = "description", source = "request.description")
    Proofs toNewEntity(ProofSubmissionRequest request, Long studentId, Activities activity);

    // 3. Update
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "studentId", ignore = true)
    @Mapping(target = "activity", ignore = true)
    @Mapping(target = "status", expression = "java(0)")
    @Mapping(target = "rejectionReason", expression = "java(null)")
    @Mapping(target = "verifiedBy", ignore = true)
    @Mapping(target = "verifiedTime", ignore = true)
    void updateEntityFromRequest(ProofSubmissionRequest request, @MappingTarget Proofs entity);
}