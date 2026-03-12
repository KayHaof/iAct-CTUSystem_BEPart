package com.example.feature.activitySchedule.mapper;

import com.example.feature.activitySchedule.dto.ActivityScheduleDto;
import com.example.feature.activitySchedule.model.ActivitySchedule;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ActivityScheduleMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activity", ignore = true)
    ActivitySchedule toEntity(ActivityScheduleDto dto);

    List<ActivitySchedule> toEntityList(List<ActivityScheduleDto> dtos);

    ActivityScheduleDto toDto(ActivitySchedule entity);

    List<ActivityScheduleDto> toDtoList(List<ActivitySchedule> entities);
}
