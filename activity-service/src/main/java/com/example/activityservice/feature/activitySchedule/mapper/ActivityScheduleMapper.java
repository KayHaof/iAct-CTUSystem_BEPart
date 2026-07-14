package com.example.activityservice.feature.activitySchedule.mapper;

import com.example.activityservice.feature.activitySchedule.dto.ActivityScheduleDto;
import com.example.activityservice.feature.activitySchedule.model.ActivitySchedule;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ActivityScheduleMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activity", ignore = true)
    @Mapping(target = "locationRef", ignore = true)
    ActivitySchedule toEntity(ActivityScheduleDto dto);

    List<ActivitySchedule> toEntityList(List<ActivityScheduleDto> dtos);

    @Mapping(target = "locationId", source = "locationRef.id")
    @Mapping(target = "locationName", source = "locationRef.name")
    @Mapping(target = "locationCode", source = "locationRef.code")
    ActivityScheduleDto toDto(ActivitySchedule entity);

    List<ActivityScheduleDto> toDtoList(List<ActivitySchedule> entities);
}
