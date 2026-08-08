package com.company.employeemanagement.service;

import com.company.employeemanagement.dto.request.LoginRequest;
import com.company.employeemanagement.dto.request.RegisterRequest;
import com.company.employeemanagement.dto.response.AuthResponse;

/**
 * Service contract for authentication operations.
 *
 * <p>Defines the public API for user registration and login without
 * exposing implementation details to controllers.
 *
 * @author Employee Management Portal Team
 */
public interface AuthService {

    /**
     * Registers a new user account with the {@code ROLE_EMPLOYEE} default role.
     *
     * <p>The plaintext password is hashed with BCrypt (strength 12) before
     * being persisted. A JWT access token is generated and returned so that
     * the user is immediately authenticated after registration.
     *
     * @param request the registration payload containing email, password,
     *                first name, and last name
     * @return an {@link AuthResponse} containing the signed JWT and user details
     * @throws com.company.employeemanagement.exception.DuplicateResourceException
     *         if the email address is already registered
     */
    AuthResponse register(RegisterRequest request);

    /**
     * Authenticates a user with the supplied credentials and issues a JWT.
     *
     * @param request the login payload containing email and password
     * @return an {@link AuthResponse} containing the signed JWT and user details
     * @throws org.springframework.security.authentication.BadCredentialsException
     *         if the email or password is incorrect
     * @throws org.springframework.security.authentication.DisabledException
     *         if the account is disabled
     * @throws org.springframework.security.authentication.LockedException
     *         if the account is locked
     */
    AuthResponse login(LoginRequest request);
}
