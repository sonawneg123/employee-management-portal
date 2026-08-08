package com.company.employeemanagement.mapper;

import com.company.employeemanagement.dto.response.LeaveRequestResponse;
import com.company.employeemanagement.entity.Employee;
import com.company.employeemanagement.entity.LeaveRequest;
import com.company.employeemanagement.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.time.temporal.ChronoUnit;

// ChronoUnit is referenced in a MapStruct expression; the fully-qualified name is
// used inside the expression string to ensure the generated class compiles correctly.

/**
 * MapStruct mapper that converts {@link LeaveRequest} entities to
 * {@link LeaveRequestResponse} DTOs.
 *
 * <p>Computed fields:
 * <ul>
 *   <li>{@code totalDays} — number of calendar days between startDate and
 *       endDate (inclusive).</li>
 *   <li>{@code employeeCode}, {@code employeeName}, {@code departmentName} —
 *       extracted from the {@link Employee} and its optional linked {@link User}.</li>
 * </ul>
 *
 * @author Employee Management Portal Team
 */
@Mapper(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface LeaveRequestMapper {

    /**
     * Maps a {@link LeaveRequest} entity to a {@link LeaveRequestResponse} DTO.
     *
     * @param leaveRequest the source entity
     * @return a populated {@link LeaveRequestResponse}
     */
    @Mapping(target = "employeeId",     source = "employee.id")
    @Mapping(target = "employeeCode",   source = "employee.employeeCode")
    @Mapping(target = "employeeName",   source = "employee", qualifiedByName = "employeeToName")
    @Mapping(target = "departmentName", source = "employee.department.name")
    @Mapping(target = "totalDays",      expression = "java(java.time.temporal.ChronoUnit.DAYS.between(leaveRequest.getStartDate(), leaveRequest.getEndDate()) + 1)")
    LeaveRequestResponse toResponse(LeaveRequest leaveRequest);

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
