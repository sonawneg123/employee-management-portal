package com.company.employeemanagement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents an application user who can authenticate via JWT and is
 * assigned one or more {@link Role} objects controlling their authorisation.
 *
 * <p>The {@code passwordHash} field stores a BCrypt-hashed password
 * (strength 12). Plaintext passwords are never persisted.
 *
 * <p>The {@code isEnabled} and {@code isLocked} flags are consumed by
 * {@link com.company.employeemanagement.security.UserDetailsServiceImpl}
 * to populate Spring Security's {@code UserDetails}.
 *
 * @author Employee Management Portal Team
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {

    /**
     * Unique email address used as the authentication principal.
     * Must be a valid email format; enforced at both the service and
     * database layers.
     */
    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    /**
     * BCrypt-hashed password with cost factor 12. Never exposed in
     * any response DTO.
     */
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    /**
     * User's first name.
     */
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    /**
     * User's last name.
     */
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    /**
     * Whether this account can be used. Disabled accounts cannot log in.
     * Defaults to {@code true}.
     */
    @Builder.Default
    @Column(name = "is_enabled", nullable = false)
    private boolean isEnabled = true;

    /**
     * Whether this account has been locked (e.g., after repeated failed
     * login attempts). Locked accounts cannot log in even if credentials
     * are correct.
     */
    @Builder.Default
    @Column(name = "is_locked", nullable = false)
    private boolean isLocked = false;

    /**
     * Set of roles assigned to this user. Eagerly fetched because
     * Spring Security requires roles during every authentication check.
     */
    @Builder.Default
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();
}
