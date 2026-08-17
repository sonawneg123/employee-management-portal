package com.company.employeemanagement.service;

import com.company.employeemanagement.dto.response.UserListResponse;
import com.company.employeemanagement.entity.Department;
import com.company.employeemanagement.entity.Employee;
import com.company.employeemanagement.entity.Role;
import com.company.employeemanagement.entity.User;
import com.company.employeemanagement.entity.enums.EmployeeStatus;
import com.company.employeemanagement.repository.EmployeeRepository;
import com.company.employeemanagement.repository.RoleRepository;
import com.company.employeemanagement.repository.UserRepository;
import com.company.employeemanagement.service.impl.AdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AdminServiceImpl} covering:
 * <ol>
 *   <li>Admin disabling a user sets linked employee status to {@code DISABLED}.</li>
 *   <li>Admin re-enabling a user restores linked employee status to {@code ACTIVE}.</li>
 *   <li>Disabling a non-ACTIVE employee does not change their status.</li>
 *   <li>Re-enabling a non-DISABLED employee does not change their status.</li>
 * </ol>
 *
 * These tests directly address the root cause of the Phase 6G Issue 1:
 * the {@code employees.status} ENUM column did not include {@code DISABLED},
 * causing MySQL to truncate the value. The fix is migration V28. These tests
 * verify that the Java service logic itself is correct so that once the DB
 * column is updated the operations succeed end-to-end.
 *
 * @author Employee Management Portal Team
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AdminServiceImpl — Disable/Re-enable Employee")
class AdminServiceDisableTest {

    @Mock private UserRepository     userRepository;
    @Mock private RoleRepository     roleRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private NotificationService notificationService;

    private AdminServiceImpl adminService;

    @BeforeEach
    void setUp() {
        adminService = new AdminServiceImpl(
                userRepository, roleRepository, employeeRepository, notificationService);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private User buildUser(UUID userId) {
        Role role = new Role();
        role.setId(UUID.randomUUID());
        role.setName("ROLE_EMPLOYEE");

        User user = User.builder()
                .email("employee@example.com")
                .passwordHash("hash")
                .firstName("Jane").lastName("Doe")
                .build();
        user.setId(userId);
        user.setRoles(Set.of(role));
        user.setEnabled(true);
        return user;
    }

    private Employee buildEmployee(UUID userId, EmployeeStatus status) {
        Department dept = new Department();
        dept.setId(UUID.randomUUID());
        dept.setName("Engineering");
        dept.setCode("ENG");

        Employee emp = Employee.builder()
                .employeeCode("EMP-001")
                .department(dept)
                .jobTitle("Engineer")
                .dateOfJoining(LocalDate.of(2024, 1, 1))
                .salary(BigDecimal.valueOf(75000))
                .status(status)
                .build();
        emp.setId(UUID.randomUUID());
        return emp;
    }

    // ── Disable tests ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("setUserEnabled(false) — disable")
    class DisableUser {

        @Test
        @DisplayName("disabling an ACTIVE employee sets status to DISABLED")
        void disable_activeEmployee_setsStatusDisabled() {
            UUID userId = UUID.randomUUID();
            User user = buildUser(userId);
            Employee employee = buildEmployee(userId, EmployeeStatus.ACTIVE);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(any())).thenReturn(user);
            when(employeeRepository.findByUserId(userId)).thenReturn(Optional.of(employee));
            when(employeeRepository.save(any())).thenReturn(employee);

            adminService.setUserEnabled(userId, false);

            assertThat(employee.getStatus()).isEqualTo(EmployeeStatus.DISABLED);
            verify(employeeRepository).save(employee);
        }

        @Test
        @DisplayName("disabling a non-ACTIVE employee does not change their status")
        void disable_nonActiveEmployee_statusUnchanged() {
            UUID userId = UUID.randomUUID();
            User user = buildUser(userId);
            // Employee is already TERMINATED — disabling user should not change status
            Employee employee = buildEmployee(userId, EmployeeStatus.TERMINATED);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(any())).thenReturn(user);
            when(employeeRepository.findByUserId(userId)).thenReturn(Optional.of(employee));

            adminService.setUserEnabled(userId, false);

            // Status should remain TERMINATED — only ACTIVE employees get DISABLED
            assertThat(employee.getStatus()).isEqualTo(EmployeeStatus.TERMINATED);
            verify(employeeRepository, never()).save(any());
        }

        @Test
        @DisplayName("disabling user with no linked employee — no employee repository save")
        void disable_noLinkedEmployee_noEmployeeSave() {
            UUID userId = UUID.randomUUID();
            User user = buildUser(userId);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(any())).thenReturn(user);
            when(employeeRepository.findByUserId(userId)).thenReturn(Optional.empty());

            adminService.setUserEnabled(userId, false);

            verify(employeeRepository, never()).save(any());
        }
    }

    // ── Re-enable tests ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("setUserEnabled(true) — re-enable")
    class ReEnableUser {

        @Test
        @DisplayName("re-enabling a DISABLED employee restores status to ACTIVE")
        void reEnable_disabledEmployee_setsStatusActive() {
            UUID userId = UUID.randomUUID();
            User user = buildUser(userId);
            user.setEnabled(false);
            Employee employee = buildEmployee(userId, EmployeeStatus.DISABLED);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(any())).thenReturn(user);
            when(employeeRepository.findByUserId(userId)).thenReturn(Optional.of(employee));
            when(employeeRepository.save(any())).thenReturn(employee);

            adminService.setUserEnabled(userId, true);

            assertThat(employee.getStatus()).isEqualTo(EmployeeStatus.ACTIVE);
            verify(employeeRepository).save(employee);
        }

        @Test
        @DisplayName("re-enabling a non-DISABLED employee does not change their status")
        void reEnable_nonDisabledEmployee_statusUnchanged() {
            UUID userId = UUID.randomUUID();
            User user = buildUser(userId);
            user.setEnabled(false);
            // Employee is ON_LEAVE — re-enabling should not touch their status
            Employee employee = buildEmployee(userId, EmployeeStatus.ON_LEAVE);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(any())).thenReturn(user);
            when(employeeRepository.findByUserId(userId)).thenReturn(Optional.of(employee));

            adminService.setUserEnabled(userId, true);

            assertThat(employee.getStatus()).isEqualTo(EmployeeStatus.ON_LEAVE);
            verify(employeeRepository, never()).save(any());
        }

        @Test
        @DisplayName("disable then re-enable cycle — status returns to ACTIVE")
        void disableAndReEnable_cycle_statusReturnsActive() {
            UUID userId = UUID.randomUUID();
            User user = buildUser(userId);
            Employee employee = buildEmployee(userId, EmployeeStatus.ACTIVE);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(any())).thenReturn(user);
            when(employeeRepository.findByUserId(userId)).thenReturn(Optional.of(employee));
            when(employeeRepository.save(any())).thenReturn(employee);

            // Disable
            adminService.setUserEnabled(userId, false);
            assertThat(employee.getStatus()).isEqualTo(EmployeeStatus.DISABLED);

            // Re-enable
            adminService.setUserEnabled(userId, true);
            assertThat(employee.getStatus()).isEqualTo(EmployeeStatus.ACTIVE);
        }
    }
}
