package com.company.employeemanagement.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an organisational department within the company.
 *
 * <p>A department owns a collection of {@link Employee} entities. Deleting
 * a department that still has active employees is restricted at the database
 * level (FK with {@code ON DELETE RESTRICT}).
 *
 * @author Employee Management Portal Team
 */
@Entity
@Table(name = "departments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Department extends BaseEntity {

    /**
     * Human-readable name of the department (e.g., "Engineering",
     * "Human Resources").
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * Short unique code identifying the department (e.g., "ENG", "HR").
     * Used as a stable external identifier.
     */
    @Column(name = "code", nullable = false, unique = true, length = 20)
    private String code;

    /**
     * List of employees assigned to this department.
     * Lazily loaded to avoid unnecessary joins on simple department lookups.
     * Mapped by the {@code department} field on {@link Employee}.
     */
    @Builder.Default
    @OneToMany(mappedBy = "department", fetch = FetchType.LAZY,
               cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<Employee> employees = new ArrayList<>();
}
