package com.company.employeemanagement.mapper;

import com.company.employeemanagement.dto.response.TaskResponse;
import com.company.employeemanagement.entity.Employee;
import com.company.employeemanagement.entity.Task;
import com.company.employeemanagement.entity.User;
import com.company.employeemanagement.entity.enums.TaskStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.time.LocalDate;

/**
 * MapStruct mapper that converts {@link Task} entities to
 * {@link TaskResponse} DTOs.
 *
 * <p>The {@code overdue} flag is derived: a task is overdue when its status
 * is not {@link TaskStatus#COMPLETED} and its {@code dueDate} is before today.
 *
 * @author Employee Management Portal Team
 */
@Mapper(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        imports = {LocalDate.class, TaskStatus.class})
public interface TaskMapper {

    /**
     * Maps a {@link Task} entity to a {@link TaskResponse} DTO.
     *
     * @param task the source entity
     * @return a populated {@link TaskResponse}
     */
    @Mapping(target = "assignedEmployeeId",   source = "assignedEmployee.id")
    @Mapping(target = "assignedEmployeeName", source = "assignedEmployee",
             qualifiedByName = "employeeToName")
    @Mapping(target = "assignedEmployeeCode", source = "assignedEmployee.employeeCode")
    @Mapping(target = "createdByEmployeeId",  source = "createdByEmployee.id")
    @Mapping(target = "createdByEmployeeName", source = "createdByEmployee",
             qualifiedByName = "employeeToName")
    @Mapping(target = "overdue",
             expression = "java(task.getStatus() != TaskStatus.COMPLETED "
                        + "&& task.getDueDate() != null "
                        + "&& task.getDueDate().isBefore(LocalDate.now()))")
    TaskResponse toResponse(Task task);

    /**
     * Derives a display name from the employee's linked user account.
     * Falls back to the employee code if no user is linked.
     *
     * @param employee the source employee entity
     * @return full name or the employee code if no user is linked
     */
    @Named("employeeToName")
    default String employeeToName(final Employee employee) {
        if (employee == null) {
            return null;
        }
        User user = employee.getUser();
        if (user == null) {
            // Fall back to firstName/lastName fields on the employee itself
            if (employee.getFirstName() != null || employee.getLastName() != null) {
                return (employee.getFirstName() != null ? employee.getFirstName() : "")
                        + " "
                        + (employee.getLastName() != null ? employee.getLastName() : "");
            }
            return employee.getEmployeeCode();
        }
        return user.getFirstName() + " " + user.getLastName();
    }
}
