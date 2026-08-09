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
 * <p>The {@code employeeCount} field is supplied by the caller as an explicit
 * parameter rather than being derived from the lazily-loaded {@code employees}
 * collection on the entity. Accessing the collection would cause an N+1 query
 * problem when mapping a page of departments, because each access would trigger
 * a separate {@code SELECT COUNT(*)} (or a full collection load) per row.
 *
 * <p>Callers should obtain the count via
 * {@link com.company.employeemanagement.repository.DepartmentRepository#countEmployeesByDepartmentId}
 * before invoking this mapper.
 *
 * @author Employee Management Portal Team
 */
@Mapper(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface DepartmentMapper {

    /**
     * Maps a {@link Department} entity and an explicitly supplied employee count
     * to a {@link DepartmentResponse} DTO.
     *
     * @param department    the source entity
     * @param employeeCount the number of employees currently assigned to the department,
     *                      obtained from a dedicated COUNT query in the repository
     * @return a fully populated {@link DepartmentResponse}
     */
    @Mapping(target = "employeeCount", source = "employeeCount")
    DepartmentResponse toResponse(Department department, long employeeCount);
}
