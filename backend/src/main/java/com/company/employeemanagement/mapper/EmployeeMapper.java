package com.company.employeemanagement.mapper;

import com.company.employeemanagement.dto.response.EmployeeResponse;
import com.company.employeemanagement.entity.Employee;
import com.company.employeemanagement.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.UUID;

/**
 * MapStruct mapper that converts between {@link Employee} entities and
 * {@link EmployeeResponse} DTOs.
 *
 * <p>The component model is set to {@code "spring"} globally via the
 * {@code -Amapstruct.defaultComponentModel=spring} compiler argument in
 * {@code pom.xml}, so all generated mapper implementations are Spring beans
 * and can be injected via constructor injection.
 *
 * <p>Fields sourced from the optional linked {@link User} (firstName, lastName,
 * email, userId) are mapped using {@link Named} helper methods that guard
 * against a {@code null} user reference, since an employee record may exist
 * before a portal account is created.
 *
 * @author Employee Management Portal Team
 */
@Mapper(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface EmployeeMapper {

    /**
     * Maps an {@link Employee} entity to an {@link EmployeeResponse} DTO.
     *
     * <p>Department fields are extracted from the non-null
     * {@link com.company.employeemanagement.entity.Department}; user details
     * (firstName, lastName, email, userId) are extracted from the optional linked
     * {@link User} via null-safe named helper methods.
     *
     * @param employee the source entity
     * @return a fully populated {@link EmployeeResponse} (null user fields
     *         will be {@code null} in the response)
     */
    @Mapping(target = "departmentId",   source = "department.id")
    @Mapping(target = "departmentName", source = "department.name")
    @Mapping(target = "userId",         source = "user", qualifiedByName = "userToId")
    @Mapping(target = "firstName",      source = "user", qualifiedByName = "userToFirstName")
    @Mapping(target = "lastName",       source = "user", qualifiedByName = "userToLastName")
    @Mapping(target = "email",          source = "user", qualifiedByName = "userToEmail")
    EmployeeResponse toResponse(Employee employee);

    /**
     * Extracts the UUID from a nullable {@link User}.
     *
     * @param user the user entity, may be {@code null}
     * @return the user's UUID, or {@code null} if the user is {@code null}
     */
    @Named("userToId")
    default UUID userToId(final User user) {
        return user == null ? null : user.getId();
    }

    /**
     * Extracts the first name from a nullable {@link User}.
     *
     * @param user the user entity, may be {@code null}
     * @return the user's first name, or {@code null}
     */
    @Named("userToFirstName")
    default String userToFirstName(final User user) {
        return user == null ? null : user.getFirstName();
    }

    /**
     * Extracts the last name from a nullable {@link User}.
     *
     * @param user the user entity, may be {@code null}
     * @return the user's last name, or {@code null}
     */
    @Named("userToLastName")
    default String userToLastName(final User user) {
        return user == null ? null : user.getLastName();
    }

    /**
     * Extracts the email address from a nullable {@link User}.
     *
     * @param user the user entity, may be {@code null}
     * @return the user's email, or {@code null}
     */
    @Named("userToEmail")
    default String userToEmail(final User user) {
        return user == null ? null : user.getEmail();
    }
}
