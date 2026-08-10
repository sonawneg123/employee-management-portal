package com.company.employeemanagement.mapper;

import com.company.employeemanagement.dto.response.AttendanceResponse;
import com.company.employeemanagement.entity.Attendance;
import com.company.employeemanagement.entity.Employee;
import com.company.employeemanagement.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * MapStruct mapper that converts {@link Attendance} entities to
 * {@link AttendanceResponse} DTOs.
 *
 * @author Employee Management Portal Team
 */
@Mapper(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface AttendanceMapper {

    /**
     * Maps an {@link Attendance} entity to an {@link AttendanceResponse} DTO.
     *
     * @param attendance the source entity
     * @return a populated {@link AttendanceResponse}
     */
    @Mapping(target = "employeeId",   source = "employee.id")
    @Mapping(target = "employeeCode", source = "employee.employeeCode")
    @Mapping(target = "employeeName", source = "employee", qualifiedByName = "employeeToName")
    AttendanceResponse toResponse(Attendance attendance);

    /**
     * Derives a display name from the employee's linked user account.
     * Falls back to the employee code if no user is linked.
     *
     * @param employee the source employee entity
     * @return full name (first + last) or the employee code if no user is linked
     */
    @Named("employeeToName")
    default String employeeToName(final Employee employee) {
        if (employee == null) {
            return null;
        }
        User user = employee.getUser();
        if (user == null) {
            return employee.getEmployeeCode();
        }
        return user.getFirstName() + " " + user.getLastName();
    }
}
