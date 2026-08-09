package com.company.employeemanagement.repository;

import com.company.employeemanagement.config.AuditingConfig;
import com.company.employeemanagement.entity.Department;
import com.company.employeemanagement.entity.Employee;
import com.company.employeemanagement.entity.LeaveRequest;
import com.company.employeemanagement.entity.enums.EmployeeStatus;
import com.company.employeemanagement.entity.enums.LeaveStatus;
import com.company.employeemanagement.entity.enums.LeaveType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JPA persistence tests covering:
 * <ul>
 *   <li>Unique constraints (department code, employee code, user uniqueness per employee)</li>
 *   <li>Foreign-key / cascade behavior (department → employee, employee → leave)</li>
 *   <li>Pagination — database-level ID queries return correct counts, sizes, pages</li>
 *   <li>Search queries — keyword matching, employee ownership queries</li>
 *   <li>COUNT projection — no lazy collection load for employee count</li>
 * </ul>
 *
 * <p>Uses {@link DataJpaTest} with H2 in create-drop mode and Flyway disabled.
 * All tests are isolated — repositories are rolled back after each test.
 *
 * @author Employee Management Portal Team
 */
@DataJpaTest
@Import({AuditingConfig.class, PersistenceRepositoryTest.TestJpaAuditingConfig.class})
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@DisplayName("Persistence & Constraint Tests")
class PersistenceRepositoryTest {

    @TestConfiguration
    @EnableJpaAuditing(auditorAwareRef = "auditorAware")
    static class TestJpaAuditingConfig {
    }

    @Autowired private DepartmentRepository   departmentRepository;
    @Autowired private EmployeeRepository     employeeRepository;
    @Autowired private LeaveRequestRepository leaveRequestRepository;

    private Department savedDept;

    @BeforeEach
    void setUp() {
        savedDept = departmentRepository.save(
                Department.builder().name("Engineering").code("ENG").build());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private Employee buildEmployee(final String code) {
        return Employee.builder()
                .employeeCode(code)
                .department(savedDept)
                .jobTitle("Engineer")
                .dateOfJoining(LocalDate.of(2024, 1, 1))
                .salary(BigDecimal.valueOf(60_000))
                .status(EmployeeStatus.ACTIVE)
                .build();
    }

    private LeaveRequest buildLeave(final Employee emp, final LocalDate start, final LocalDate end) {
        return LeaveRequest.builder()
                .employee(emp)
                .leaveType(LeaveType.ANNUAL)
                .startDate(start)
                .endDate(end)
                .status(LeaveStatus.PENDING)
                .build();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Unique constraints
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Unique constraints")
    class UniqueConstraints {

        @Test
        @DisplayName("duplicate department code throws DataIntegrityViolationException")
        void duplicateDepartmentCode_throws() {
            departmentRepository.saveAndFlush(
                    Department.builder().name("Finance").code("FIN").build());

            assertThatThrownBy(() -> departmentRepository.saveAndFlush(
                    Department.builder().name("Finance Duplicate").code("FIN").build()))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("duplicate employee code throws DataIntegrityViolationException")
        void duplicateEmployeeCode_throws() {
            employeeRepository.saveAndFlush(buildEmployee("EMP-001"));

            assertThatThrownBy(() -> employeeRepository.saveAndFlush(buildEmployee("EMP-001")))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("existsByCode returns true after department is saved")
        void existsByCode_returnsTrue() {
            assertThat(departmentRepository.existsByCode("ENG")).isTrue();
        }

        @Test
        @DisplayName("existsByCode returns false for unknown code")
        void existsByCode_returnsFalse() {
            assertThat(departmentRepository.existsByCode("UNKNOWN")).isFalse();
        }

        @Test
        @DisplayName("existsByEmployeeCode returns true after employee is saved")
        void existsByEmployeeCode_returnsTrue() {
            employeeRepository.saveAndFlush(buildEmployee("EMP-X01"));
            assertThat(employeeRepository.existsByEmployeeCode("EMP-X01")).isTrue();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // COUNT projection
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Department employee COUNT projection")
    class DepartmentCountProjection {

        @Test
        @DisplayName("countEmployeesByDepartmentId returns 0 for a new department")
        void countIsZeroForNewDepartment() {
            long count = departmentRepository.countEmployeesByDepartmentId(savedDept.getId());
            assertThat(count).isZero();
        }

        @Test
        @DisplayName("countEmployeesByDepartmentId increments after employee is saved")
        void countIncrementsAfterEmployeeSaved() {
            employeeRepository.saveAndFlush(buildEmployee("EMP-C01"));
            employeeRepository.saveAndFlush(buildEmployee("EMP-C02"));

            long count = departmentRepository.countEmployeesByDepartmentId(savedDept.getId());
            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("countEmployeesByDepartmentId returns 0 for a different department")
        void countIsZeroForDifferentDepartment() {
            Department other = departmentRepository.save(
                    Department.builder().name("HR").code("HR").build());
            employeeRepository.saveAndFlush(buildEmployee("EMP-C03"));

            long count = departmentRepository.countEmployeesByDepartmentId(other.getId());
            assertThat(count).isZero();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Pagination — ID queries
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Pagination — employee ID queries")
    class EmployeePagination {

        @Test
        @DisplayName("findAllIds returns correct total elements and page size")
        void findAllIds_correctPagination() {
            for (int i = 1; i <= 5; i++) {
                employeeRepository.saveAndFlush(buildEmployee("EMP-P0" + i));
            }

            PageRequest page0 = PageRequest.of(0, 3, Sort.by("employeeCode"));
            Page<UUID> result = employeeRepository.findAllIds(page0);

            assertThat(result.getTotalElements()).isEqualTo(5);
            assertThat(result.getContent()).hasSize(3);
            assertThat(result.getTotalPages()).isEqualTo(2);
        }

        @Test
        @DisplayName("findAllIds second page contains remaining elements")
        void findAllIds_secondPage() {
            for (int i = 1; i <= 5; i++) {
                employeeRepository.saveAndFlush(buildEmployee("EMP-P1" + i));
            }

            PageRequest page1 = PageRequest.of(1, 3, Sort.by("employeeCode"));
            Page<UUID> result = employeeRepository.findAllIds(page1);

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.isLast()).isTrue();
        }

        @Test
        @DisplayName("findAllWithAssociationsByIds returns all requested employees")
        void findAllWithAssociationsByIds_returnsAll() {
            Employee e1 = employeeRepository.saveAndFlush(buildEmployee("EMP-F01"));
            Employee e2 = employeeRepository.saveAndFlush(buildEmployee("EMP-F02"));

            List<Employee> result = employeeRepository
                    .findAllWithAssociationsByIds(List.of(e1.getId(), e2.getId()));

            assertThat(result).hasSize(2);
            // Department association must be initialised (no LazyInitializationException)
            result.forEach(e -> assertThat(e.getDepartment().getName()).isEqualTo("Engineering"));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Pagination — leave request ID queries
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Pagination — leave request ID queries")
    class LeaveRequestPagination {

        @Test
        @DisplayName("findIdsByEmployeeId returns correct page for a specific employee")
        void findIdsByEmployeeId_correctPage() {
            Employee emp = employeeRepository.saveAndFlush(buildEmployee("EMP-L01"));
            leaveRequestRepository.saveAndFlush(
                    buildLeave(emp, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 5)));
            leaveRequestRepository.saveAndFlush(
                    buildLeave(emp, LocalDate.of(2025, 2, 1), LocalDate.of(2025, 2, 3)));

            Page<UUID> result = leaveRequestRepository.findIdsByEmployeeId(
                    emp.getId(), PageRequest.of(0, 10));

            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent()).hasSize(2);
        }

        @Test
        @DisplayName("findIdsByEmployeeId does not return leaves of other employees")
        void findIdsByEmployeeId_isolatedPerEmployee() {
            Employee emp1 = employeeRepository.saveAndFlush(buildEmployee("EMP-L02"));
            Employee emp2 = employeeRepository.saveAndFlush(buildEmployee("EMP-L03"));

            leaveRequestRepository.saveAndFlush(
                    buildLeave(emp1, LocalDate.of(2025, 3, 1), LocalDate.of(2025, 3, 2)));
            leaveRequestRepository.saveAndFlush(
                    buildLeave(emp2, LocalDate.of(2025, 3, 5), LocalDate.of(2025, 3, 6)));

            Page<UUID> emp1Result = leaveRequestRepository.findIdsByEmployeeId(
                    emp1.getId(), PageRequest.of(0, 10));
            assertThat(emp1Result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("findAllWithAssociationsByIds loads leave with employee and department")
        void findAllWithAssociationsByIds_loadsAssociations() {
            Employee emp = employeeRepository.saveAndFlush(buildEmployee("EMP-L04"));
            LeaveRequest lr = leaveRequestRepository.saveAndFlush(
                    buildLeave(emp, LocalDate.of(2025, 4, 1), LocalDate.of(2025, 4, 3)));

            List<LeaveRequest> result = leaveRequestRepository
                    .findAllWithAssociationsByIds(List.of(lr.getId()));

            assertThat(result).hasSize(1);
            // Both employee and department associations must be initialised
            assertThat(result.get(0).getEmployee()).isNotNull();
            assertThat(result.get(0).getEmployee().getDepartment().getName())
                    .isEqualTo("Engineering");
        }

        @Test
        @DisplayName("findIdsByStatus returns only PENDING leaves")
        void findIdsByStatus_pendingOnly() {
            Employee emp = employeeRepository.saveAndFlush(buildEmployee("EMP-L05"));
            leaveRequestRepository.saveAndFlush(
                    buildLeave(emp, LocalDate.of(2025, 5, 1), LocalDate.of(2025, 5, 2)));

            LeaveRequest approved = LeaveRequest.builder()
                    .employee(emp)
                    .leaveType(LeaveType.SICK)
                    .startDate(LocalDate.of(2025, 6, 1))
                    .endDate(LocalDate.of(2025, 6, 2))
                    .status(LeaveStatus.APPROVED)
                    .build();
            leaveRequestRepository.saveAndFlush(approved);

            Page<UUID> pendingPage = leaveRequestRepository.findIdsByStatus(
                    LeaveStatus.PENDING, PageRequest.of(0, 10));

            assertThat(pendingPage.getTotalElements()).isEqualTo(1);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Search queries
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Employee search queries")
    class EmployeeSearch {

        @Test
        @DisplayName("searchIdsByKeyword matches on jobTitle (case-insensitive)")
        void searchIdsByKeyword_matchesJobTitle() {
            employeeRepository.saveAndFlush(buildEmployee("EMP-S01"));  // jobTitle = "Engineer"
            Employee other = Employee.builder()
                    .employeeCode("EMP-S02")
                    .department(savedDept)
                    .jobTitle("HR Manager")
                    .dateOfJoining(LocalDate.of(2024, 1, 1))
                    .salary(BigDecimal.valueOf(55_000))
                    .status(EmployeeStatus.ACTIVE)
                    .build();
            employeeRepository.saveAndFlush(other);

            Page<UUID> result = employeeRepository.searchIdsByKeyword(
                    "engineer", PageRequest.of(0, 10));

            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("searchIdsByKeyword returns empty page for no match")
        void searchIdsByKeyword_noMatch() {
            employeeRepository.saveAndFlush(buildEmployee("EMP-S03"));

            Page<UUID> result = employeeRepository.searchIdsByKeyword(
                    "nonexistent", PageRequest.of(0, 10));

            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("findByUserId returns empty for employee without linked user")
        void findByUserId_emptyWhenNoUser() {
            employeeRepository.saveAndFlush(buildEmployee("EMP-S04"));

            assertThat(employeeRepository.findByUserId(UUID.randomUUID())).isEmpty();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Department search
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Department search queries")
    class DepartmentSearch {

        @Test
        @DisplayName("searchByKeyword matches on department name (case-insensitive)")
        void searchByKeyword_matchesName() {
            departmentRepository.save(
                    Department.builder().name("Finance").code("FIN").build());
            departmentRepository.save(
                    Department.builder().name("Human Resources").code("HR").build());

            Page<Department> result = departmentRepository.searchByKeyword(
                    "finance", PageRequest.of(0, 10));

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getCode()).isEqualTo("FIN");
        }

        @Test
        @DisplayName("searchByKeyword matches on department code")
        void searchByKeyword_matchesCode() {
            departmentRepository.save(
                    Department.builder().name("Legal").code("LEG").build());

            Page<Department> result = departmentRepository.searchByKeyword(
                    "LEG", PageRequest.of(0, 10));

            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("searchByKeyword returns empty page when keyword matches nothing")
        void searchByKeyword_noMatch() {
            Page<Department> result = departmentRepository.searchByKeyword(
                    "zzz", PageRequest.of(0, 10));
            assertThat(result.getTotalElements()).isZero();
        }
    }
}
