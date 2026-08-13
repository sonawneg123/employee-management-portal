package com.company.employeemanagement.service;

import com.company.employeemanagement.config.JwtProperties;
import com.company.employeemanagement.dto.request.LoginRequest;
import com.company.employeemanagement.dto.request.RegisterRequest;
import com.company.employeemanagement.dto.response.AuthResponse;
import com.company.employeemanagement.entity.Department;
import com.company.employeemanagement.entity.Employee;
import com.company.employeemanagement.entity.Role;
import com.company.employeemanagement.entity.User;
import com.company.employeemanagement.entity.enums.EmployeeStatus;
import com.company.employeemanagement.exception.DuplicateResourceException;
import com.company.employeemanagement.repository.DepartmentRepository;
import com.company.employeemanagement.repository.EmployeeRepository;
import com.company.employeemanagement.repository.RoleRepository;
import com.company.employeemanagement.repository.UserRepository;
import com.company.employeemanagement.security.JwtService;
import com.company.employeemanagement.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuthServiceImpl}.
 *
 * <p>All external dependencies are mocked using Mockito. No Spring context
 * is loaded, keeping test execution fast.
 *
 * @author Employee Management Portal Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl")
class AuthServiceTest {

    @Mock private UserRepository         userRepository;
    @Mock private RoleRepository         roleRepository;
    @Mock private EmployeeRepository     employeeRepository;
    @Mock private DepartmentRepository   departmentRepository;
    @Mock private PasswordEncoder        passwordEncoder;
    @Mock private JwtService             jwtService;
    @Mock private AuthenticationManager  authenticationManager;
    @Mock private UserDetailsService     userDetailsService;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties(
                "TestSecretKeyThatIsAtLeast256BitsLongForUnitTests!!",
                86_400_000L,
                604_800_000L
        );
        authService = new AuthServiceImpl(
                userRepository, roleRepository, employeeRepository, departmentRepository,
                passwordEncoder, jwtService, authenticationManager, userDetailsService, props
        );
    }

    // ──────────────────────────────────────────────────────────────────────────
    // register()
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        @DisplayName("returns AuthResponse with token on successful registration")
        void successfulRegistration() {
            // arrange
            RegisterRequest request = new RegisterRequest(
                    "john@example.com", "SecureP@ss1", "John", "Doe");

            Role role = Role.builder().name("ROLE_EMPLOYEE").build();

            UUID userId = UUID.randomUUID();
            User savedUser = User.builder()
                    .email("john@example.com")
                    .firstName("John")
                    .lastName("Doe")
                    .passwordHash("hashed")
                    .roles(Set.of(role))
                    .build();
            // Simulate JPA-assigned ID so that employee auto-creation receives a non-null user ID
            savedUser.setId(userId);

            // Stub a department so Employee record auto-creation does not abort
            Department dept = Department.builder().name("General").code("GEN").build();

            Employee savedEmployee = Employee.builder()
                    .user(savedUser)
                    .employeeCode("REG-0000000001")
                    .department(dept)
                    .jobTitle("Employee")
                    .status(EmployeeStatus.ACTIVE)
                    .build();

            UserDetails userDetails = org.springframework.security.core.userdetails.User
                    .withUsername("john@example.com")
                    .password("hashed")
                    .authorities(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))
                    .build();

            when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
            when(roleRepository.findByName("ROLE_EMPLOYEE")).thenReturn(Optional.of(role));
            when(passwordEncoder.encode("SecureP@ss1")).thenReturn("hashed");
            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            // Employee auto-creation stubs — use any() to match even if user ID is null in tests
            when(employeeRepository.findByUserId(any())).thenReturn(Optional.empty());
            when(departmentRepository.findByCode("GEN")).thenReturn(Optional.of(dept));
            when(employeeRepository.existsByEmployeeCode(anyString())).thenReturn(false);
            when(employeeRepository.save(any(Employee.class))).thenReturn(savedEmployee);
            when(userDetailsService.loadUserByUsername("john@example.com")).thenReturn(userDetails);
            when(jwtService.generateToken(userDetails)).thenReturn("mock.jwt.token");

            // act
            AuthResponse response = authService.register(request);

            // assert
            assertThat(response.accessToken()).isEqualTo("mock.jwt.token");
            assertThat(response.tokenType()).isEqualTo("Bearer");
            assertThat(response.email()).isEqualTo("john@example.com");
            assertThat(response.firstName()).isEqualTo("John");
            assertThat(response.roles()).containsExactly("ROLE_EMPLOYEE");
            verify(userRepository).save(any(User.class));
            // Verify that an Employee record was auto-created
            verify(employeeRepository).save(any(Employee.class));
        }

        @Test
        @DisplayName("throws DuplicateResourceException when email is already registered")
        void throwsWhenEmailAlreadyExists() {
            RegisterRequest request = new RegisterRequest(
                    "duplicate@example.com", "Pass1234!", "Jane", "Doe");

            when(userRepository.existsByEmail("duplicate@example.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("duplicate@example.com");

            verify(userRepository, never()).save(any());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // login()
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("returns AuthResponse with token on successful login")
        void successfulLogin() {
            LoginRequest request = new LoginRequest("alice@example.com", "password123");

            Role role = Role.builder().name("ROLE_ADMIN").build();
            User user = User.builder()
                    .email("alice@example.com")
                    .firstName("Alice")
                    .lastName("Smith")
                    .passwordHash("hashed")
                    .roles(Set.of(role))
                    .build();

            UserDetails userDetails = org.springframework.security.core.userdetails.User
                    .withUsername("alice@example.com")
                    .password("hashed")
                    .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                    .build();

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(null);
            when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
            when(userDetailsService.loadUserByUsername("alice@example.com")).thenReturn(userDetails);
            when(jwtService.generateToken(userDetails)).thenReturn("admin.jwt.token");

            AuthResponse response = authService.login(request);

            assertThat(response.accessToken()).isEqualTo("admin.jwt.token");
            assertThat(response.email()).isEqualTo("alice@example.com");
            assertThat(response.roles()).containsExactly("ROLE_ADMIN");
        }

        @Test
        @DisplayName("propagates BadCredentialsException when credentials are wrong")
        void propagatesBadCredentials() {
            LoginRequest request = new LoginRequest("wrong@example.com", "wrongpass");

            doThrow(new BadCredentialsException("Bad credentials"))
                    .when(authenticationManager)
                    .authenticate(any(UsernamePasswordAuthenticationToken.class));

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BadCredentialsException.class);
        }
    }
}
