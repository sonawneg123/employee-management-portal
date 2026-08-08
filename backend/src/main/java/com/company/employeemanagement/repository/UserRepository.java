package com.company.employeemanagement.repository;

import com.company.employeemanagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link User} entities.
 *
 * <p>Provides standard CRUD operations plus the email-based lookup required
 * by Spring Security's {@code UserDetailsService}.
 *
 * @author Employee Management Portal Team
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Finds a user by their email address (case-sensitive).
     *
     * @param email the email address to search for
     * @return an {@link Optional} containing the matching {@link User},
     *         or empty if no user with that email exists
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks whether a user with the given email address already exists.
     *
     * @param email the email address to check
     * @return {@code true} if a user with that email exists, {@code false} otherwise
     */
    boolean existsByEmail(String email);
}
