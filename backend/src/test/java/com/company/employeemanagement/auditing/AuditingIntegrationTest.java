package com.company.employeemanagement.auditing;

import com.company.employeemanagement.config.AuditingConfig;
import com.company.employeemanagement.entity.Department;
import com.company.employeemanagement.entity.Employee;
import com.company.employeemanagement.entity.LeaveRequest;
import com.company.employeemanagement.entity.enums.EmployeeStatus;
import com.company.employeemanagement.entity.enums.LeaveStatus;
import com.company.employeemanagement.entity.enums.LeaveType;
import com.company.employeemanagement.repository.DepartmentRepository;
import com.company.employeemanagement.repository.EmployeeRepository;
import com.company.employeemanagement.repository.LeaveRequestRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Spring Data JPA Auditing behaviour.
 *
 * <p>Uses {@link DataJpaTest} to boot only the JPA slice with an in-memory
 * H2 database, keeping tests fast and isolated from infrastructure concerns.
 *
 * <p>{@link AuditingConfig} is imported to provide the {@code AuditorAware<String>}
 * bean, and {@link EnableJpaAuditing} is activated via the inner
 * {@link TestJpaAuditingConfig} so the {@link DataJpaTest} slice picks up
 * auditing without loading the full application context.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>{@code createdAt} populated automatically on persist.</li>
 *   <li>{@code updatedAt} populated automatically on persist and updated on merge.</li>
 *   <li>{@code createdBy} set to the authenticated principal's name on persist.</li>
 *   <li>{@code updatedBy} set to the authenticated principal's name on persist and update.</li>
 *   <li>{@code createdAt} and {@code createdBy} unchanged after an update.</li>
 *   <li>Unauthenticated / system operations fall back to {@code "SYSTEM"}.</li>
 * </ul>
 *
 * @author Employee Management Portal Team
 */
@DataJpaTest
@Import({AuditingConfig.class, AuditingIntegrationTest.TestJpaAuditingConfig.class})
@TestPropertySource(properties = {
        // Use H2 create-drop so the test schema is built from entities
        "spring.jpa.hibernate.ddl-auto=create-drop",
        // Disable Flyway for the JPA slice — schema comes from Hibernate
        "spring.flyway.enabled=false"
})
@DisplayName("JPA Auditing — BaseEntity audit fields")
class AuditingIntegrationTest {

    /**
     * Activates {@link EnableJpaAuditing} within the {@link DataJpaTest} slice.
     *
     * <p>{@link DataJpaTest} does not load {@code @SpringBootApplication}, so
     * the {@code @EnableJpaAuditing} annotation on
     * {@link com.company.employeemanagement.EmployeeManagementApplication} is
     * not in effect. This inner {@link TestConfiguration} re-enables it
     * for the test slice, pointing at the same {@code auditorAware} bean
     * provided by {@link AuditingConfig}.
     */
    @TestConfiguration
    @EnableJpaAuditing(auditorAwareRef = "auditorAware")
    static class TestJpaAuditingConfig {
    }

    @Autowired private DepartmentRepository  departmentRepository;
    @Autowired private EmployeeRepository    employeeRepository;
    @Autowired private LeaveRequestRepository leaveRequestRepository;

    /** Reusable department saved in tests that need an employee. */
    private Department savedDepartment;

    @BeforeEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
        // Persist a department that employee tests can reference
        savedDepartment = departmentRepository.save(
                Department.builder().name("Engineering").code("ENG").build());
    }

    @AfterEach
    void resetSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper — authenticate a mock principal in the SecurityContext
    // ─────────────────────────────────────────────────────────────────────────

    private void authenticateAs(final String email) {
        var auth = new UsernamePasswordAuthenticationToken(
                email, null,
                List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private Employee buildEmployee(final String code) {
        return Employee.builder()
                .employeeCode(code)
                .department(savedDepartment)
                .jobTitle("Engineer")
                .dateOfJoining(LocalDate.of(2024, 1, 1))
                .salary(BigDecimal.valueOf(50_000))
                .status(EmployeeStatus.ACTIVE)
                .build();
    }

    private LeaveRequest buildLeaveRequest(final Employee employee) {
        return LeaveRequest.builder()
                .employee(employee)
                .leaveType(LeaveType.ANNUAL)
                .startDate(LocalDate.of(2025, 8, 1))
                .endDate(LocalDate.of(2025, 8, 5))
                .status(LeaveStatus.PENDING)
                .build();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // createdAt — populated on persist
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("createdAt")
    class CreatedAt {

        @Test
        @DisplayName("populated automatically when Department is persisted")
        void department_createdAtPopulatedOnSave() {
            Department dept = Department.builder().name("Finance").code("FIN").build();
            Department saved = departmentRepository.save(dept);
            assertThat(saved.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("populated automatically when Employee is persisted")
        void employee_createdAtPopulatedOnSave() {
            Employee saved = employeeRepository.save(buildEmployee("EMP-A01"));
            assertThat(saved.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("populated automatically when LeaveRequest is persisted")
        void leaveRequest_createdAtPopulatedOnSave() {
            Employee employee = employeeRepository.save(buildEmployee("EMP-A02"));
            LeaveRequest saved = leaveRequestRepository.save(buildLeaveRequest(employee));
            assertThat(saved.getCreatedAt()).isNotNull();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // updatedAt — populated on persist AND updated on merge
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("updatedAt")
    class UpdatedAt {

        @Test
        @DisplayName("populated automatically on first save")
        void department_updatedAtPopulatedOnSave() {
            Department saved = departmentRepository.save(
                    Department.builder().name("Legal").code("LEG").build());
            assertThat(saved.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("updated when Department is modified and re-saved")
        void department_updatedAtChangesOnUpdate() throws InterruptedException {
            Department dept = departmentRepository.save(
                    Department.builder().name("Marketing").code("MKT").build());
            var originalUpdatedAt = dept.getUpdatedAt();

            // Ensure a measurable time difference — JPA auditing uses wall-clock time
            Thread.sleep(50);

            dept.setName("Marketing & Communications");
            Department updated = departmentRepository.saveAndFlush(dept);

            assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(originalUpdatedAt);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // createdBy — set from authenticated principal on persist
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("createdBy")
    class CreatedBy {

        @Test
        @DisplayName("set to authenticated principal's email on persist")
        void department_createdByPopulatedFromPrincipal() {
            authenticateAs("hr@example.com");

            Department saved = departmentRepository.save(
                    Department.builder().name("Operations").code("OPS").build());

            assertThat(saved.getCreatedBy()).isEqualTo("hr@example.com");
        }

        @Test
        @DisplayName("set to 'SYSTEM' when no authentication is present")
        void department_createdByFallsBackToSystem_whenUnauthenticated() {
            // No authentication set — SecurityContextHolder is empty
            Department saved = departmentRepository.save(
                    Department.builder().name("Procurement").code("PROC").build());

            assertThat(saved.getCreatedBy()).isEqualTo("SYSTEM");
        }

        @Test
        @DisplayName("set from authenticated principal on Employee persist")
        void employee_createdByPopulatedFromPrincipal() {
            authenticateAs("admin@example.com");

            Employee saved = employeeRepository.save(buildEmployee("EMP-C01"));
            assertThat(saved.getCreatedBy()).isEqualTo("admin@example.com");
        }

        @Test
        @DisplayName("set from authenticated principal on LeaveRequest persist")
        void leaveRequest_createdByPopulatedFromPrincipal() {
            authenticateAs("employee@example.com");

            Employee employee = employeeRepository.save(buildEmployee("EMP-C02"));
            LeaveRequest saved = leaveRequestRepository.save(buildLeaveRequest(employee));

            assertThat(saved.getCreatedBy()).isEqualTo("employee@example.com");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // updatedBy — set from authenticated principal on persist and update
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("updatedBy")
    class UpdatedBy {

        @Test
        @DisplayName("set to authenticated principal's email on update")
        void department_updatedBySetOnUpdate() {
            authenticateAs("hr@example.com");
            Department dept = departmentRepository.save(
                    Department.builder().name("IT").code("IT").build());

            // Switch actor for the update
            authenticateAs("admin@example.com");
            dept.setName("Information Technology");
            Department updated = departmentRepository.saveAndFlush(dept);

            assertThat(updated.getUpdatedBy()).isEqualTo("admin@example.com");
        }

        @Test
        @DisplayName("set to 'SYSTEM' on update when unauthenticated")
        void department_updatedByFallsBackToSystem_whenUnauthenticated() {
            Department dept = departmentRepository.save(
                    Department.builder().name("Compliance").code("COMP").build());

            dept.setName("Compliance & Risk");
            Department updated = departmentRepository.saveAndFlush(dept);

            assertThat(updated.getUpdatedBy()).isEqualTo("SYSTEM");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Immutability — createdAt and createdBy must not change after update
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Immutability — createdAt and createdBy unchanged after update")
    class Immutability {

        @Test
        @DisplayName("createdAt is unchanged after Department update")
        void department_createdAtUnchangedAfterUpdate() throws InterruptedException {
            authenticateAs("hr@example.com");
            Department dept = departmentRepository.saveAndFlush(
                    Department.builder().name("Strategy").code("STRAT").build());
            var originalCreatedAt = dept.getCreatedAt();

            Thread.sleep(50);

            authenticateAs("admin@example.com");
            dept.setName("Strategy & Innovation");
            Department updated = departmentRepository.saveAndFlush(dept);

            assertThat(updated.getCreatedAt()).isEqualTo(originalCreatedAt);
        }

        @Test
        @DisplayName("createdBy is unchanged after Department update")
        void department_createdByUnchangedAfterUpdate() {
            authenticateAs("hr@example.com");
            Department dept = departmentRepository.saveAndFlush(
                    Department.builder().name("Security").code("SEC").build());
            var originalCreatedBy = dept.getCreatedBy();

            authenticateAs("admin@example.com");
            dept.setName("Security Operations");
            Department updated = departmentRepository.saveAndFlush(dept);

            assertThat(updated.getCreatedBy()).isEqualTo(originalCreatedBy);
            assertThat(updated.getCreatedBy()).isEqualTo("hr@example.com");
        }

        @Test
        @DisplayName("createdAt is unchanged after Employee update")
        void employee_createdAtUnchangedAfterUpdate() throws InterruptedException {
            authenticateAs("hr@example.com");
            Employee employee = employeeRepository.saveAndFlush(buildEmployee("EMP-I01"));
            var originalCreatedAt = employee.getCreatedAt();

            Thread.sleep(50);

            authenticateAs("admin@example.com");
            employee.setJobTitle("Senior Engineer");
            Employee updated = employeeRepository.saveAndFlush(employee);

            assertThat(updated.getCreatedAt()).isEqualTo(originalCreatedAt);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Unauthenticated / system operations — must not crash
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Unauthenticated / system operations do not fail")
    class SystemOperations {

        @Test
        @DisplayName("Department persisted without authentication succeeds")
        void department_persistWithoutAuth_succeeds() {
            // No auth set — should fall back to SYSTEM and not throw
            Department saved = departmentRepository.save(
                    Department.builder().name("Support").code("SUP").build());

            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(saved.getCreatedBy()).isEqualTo("SYSTEM");
        }

        @Test
        @DisplayName("Employee persisted without authentication succeeds")
        void employee_persistWithoutAuth_succeeds() {
            Employee saved = employeeRepository.save(buildEmployee("EMP-S01"));

            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(saved.getCreatedBy()).isEqualTo("SYSTEM");
        }

        @Test
        @DisplayName("LeaveRequest persisted without authentication succeeds")
        void leaveRequest_persistWithoutAuth_succeeds() {
            Employee employee = employeeRepository.save(buildEmployee("EMP-S02"));
            LeaveRequest saved = leaveRequestRepository.save(buildLeaveRequest(employee));

            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(saved.getCreatedBy()).isEqualTo("SYSTEM");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Audit fields are server-controlled — not settable through entity directly
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Audit fields are server-controlled")
    class ServerControlled {

        @Test
        @DisplayName("manually set createdBy is overwritten by AuditorAware on first save")
        void createdBy_isOverwrittenByAuditorAware() {
            authenticateAs("actual@example.com");

            Department dept = Department.builder().name("Research").code("RES").build();
            // Attempt to manually set — should be overridden by auditing infrastructure
            dept.setCreatedBy("tampered-value");

            Department saved = departmentRepository.saveAndFlush(dept);

            // Spring Data auditing sets createdBy from AuditorAware, overwriting the manual value
            assertThat(saved.getCreatedBy()).isEqualTo("actual@example.com");
        }

        @Test
        @DisplayName("updatedAt is never null after a save, even for a new entity")
        void updatedAt_neverNullAfterSave() {
            authenticateAs("hr@example.com");
            Employee saved = employeeRepository.save(buildEmployee("EMP-T01"));
            assertThat(saved.getUpdatedAt()).isNotNull();
        }
    }
}
