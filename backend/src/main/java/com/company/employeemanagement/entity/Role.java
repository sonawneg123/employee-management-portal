package com.company.employeemanagement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a security role that can be assigned to one or many {@link User} entities.
 *
 * <p>Roles follow the Spring Security convention of being prefixed with {@code ROLE_}
 * (e.g., {@code ROLE_ADMIN}, {@code ROLE_HR}, {@code ROLE_MANAGER},
 * {@code ROLE_EMPLOYEE}).
 *
 * <p>Seeded by Flyway migration {@code V1__init_schema.sql}.
 *
 * @author Employee Management Portal Team
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Role extends BaseEntity {

    /**
     * Unique role name, prefixed with {@code ROLE_} as required by Spring Security.
     * Examples: {@code ROLE_ADMIN}, {@code ROLE_HR}, {@code ROLE_MANAGER},
     * {@code ROLE_EMPLOYEE}.
     */
    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;
}
