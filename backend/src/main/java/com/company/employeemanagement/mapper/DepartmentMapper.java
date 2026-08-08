package com.company.employeemanagement.mapper;

import com.company.employeemanagement.dto.response.DepartmentResponse;
import com.company.employeemanagement.entity.Department;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * MapStruct mapper that converts between {@link Department} entities and
 * {@link DepartmentResponse} DTOs.
 *
 * <p>The {@code employeeCount} field is mapped from the size of the
 * department's {@code employees} collection. Because the collection is
 * lazily loaded, the caller must ensure the session is open (or the
 * collection has been fetched) before invoking this mapper.
 *
 * @author Employee Management Portal Team
 */
@Mapper(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface DepartmentMapper {

    /**
     * Maps a {@link Department} entity to a {@link DepartmentResponse} DTO.
     *
     * @param department the source entity
     * @return a fully populated {@link DepartmentResponse}
     */
    @Mapping(target = "employeeCount", expression = "java(department.getEmployees().size())")
    DepartmentResponse toResponse(Department department);
}
