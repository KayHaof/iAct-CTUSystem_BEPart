package com.example.feature.user_profile.mapper;

import com.example.feature.classes.model.Clazzes;
import com.example.feature.user_profile.dto.CreateProfileDto;
import com.example.feature.user_profile.dto.ProfileDto;
import com.example.feature.user_profile.dto.UserUpdateRequest;
import com.example.feature.user_profile.model.DepartmentProfile;
import com.example.feature.user_profile.model.StudentProfile;
import org.mapstruct.*;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface UserProfileMapper {

    // 1. Map cho Sinh viên
    @Mapping(target = "classId", source = "clazz.id")
    @Mapping(target = "classCode", source = "clazz.classCode")
    @Mapping(target = "className", source = "clazz.name")
    @Mapping(target = "departmentId", expression = "java(studentProfile.getClazz() != null && studentProfile.getClazz().getMajor() != null && studentProfile.getClazz().getMajor().getDepartment() != null ? studentProfile.getClazz().getMajor().getDepartment().getId() : null)")
    @Mapping(target = "departmentName", expression = "java(studentProfile.getClazz() != null && studentProfile.getClazz().getMajor() != null && studentProfile.getClazz().getMajor().getDepartment() != null ? studentProfile.getClazz().getMajor().getDepartment().getName() : null)")
    ProfileDto toDto(StudentProfile studentProfile);

    // 2. Map cho Khoa
    @Mapping(target = "departmentId", source = "department.id")
    @Mapping(target = "departmentName", source = "department.name")
    @Mapping(target = "studentCode", ignore = true)
    @Mapping(target = "birthday", ignore = true)
    @Mapping(target = "classId", ignore = true)
    ProfileDto toDto(DepartmentProfile departmentProfile);

    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "clazz", ignore = true)
    void updateStudent(UserUpdateRequest request, @MappingTarget StudentProfile studentProfile);

    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "department", ignore = true)
    void updateDepartment(UserUpdateRequest request, @MappingTarget DepartmentProfile departmentProfile);

    @Mapping(target = "clazz", expression = "java(mapClassIdToClazz(dto.getClassId()))")
    StudentProfile toStudentProfile(CreateProfileDto dto);

    default Clazzes mapClassIdToClazz(Long classId) {
        if (classId == null) {
            return null;
        }
        Clazzes clazz = new Clazzes();
        clazz.setId(classId);
        return clazz;
    }
}