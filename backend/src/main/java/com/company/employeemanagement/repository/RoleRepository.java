package com.company.employeemanagement.repository;

import com.company.employeemanagement.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Role} entities.
 *
 * <p>Provides standard CRUD operations inherited from {@link JpaRepository}
 * plus a name-based lookup used during user registration to resolve
 * the default {@code ROLE_EMPLOYEE} role.
 *
 * @author Employee Management Portal Team
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    /**
     * Finds a role by its exact name.
     *
     * @param name the role name to search for (e.g., {@code "ROLE_EMPLOYEE"})
     * @return an {@link Optional} containing the matching {@link Role},
     *         or empty if no role with that name exists
     */
    Optional<Role> findByName(String name);
}
