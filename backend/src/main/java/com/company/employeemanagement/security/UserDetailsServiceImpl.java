package com.company.employeemanagement.security;

import com.company.employeemanagement.entity.User;
import com.company.employeemanagement.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Spring Security {@link UserDetailsService} implementation that loads
 * user credentials and authorities from the relational database.
 *
 * <p>The "username" in Spring Security's terminology corresponds to the
 * user's email address in this application. The loaded {@link UserDetails}
 * carries:
 * <ul>
 *   <li>The BCrypt-hashed password.</li>
 *   <li>A set of {@link SimpleGrantedAuthority} objects derived from the
 *       user's {@link com.company.employeemanagement.entity.Role} set.</li>
 *   <li>The {@code enabled} and {@code non-locked} flags sourced from the
 *       {@link User} entity.</li>
 * </ul>
 *
 * <p>The method is annotated with {@link Transactional} to ensure that the
 * EAGER-fetched roles are loaded within the same session.
 *
 * @author Employee Management Portal Team
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Constructs the service with its required repository dependency.
     *
     * @param userRepository repository for loading {@link User} entities
     */
    public UserDetailsServiceImpl(final UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Loads a user by their email address (the principal name used in JWTs).
     *
     * @param username the email address of the user to load
     * @return a fully populated {@link UserDetails} instance
     * @throws UsernameNotFoundException if no user with the given email exists
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(final String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with email: " + username));

        Set<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toSet());

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(user.isLocked())
                .credentialsExpired(false)
                .disabled(!user.isEnabled())
                .build();
    }
}
