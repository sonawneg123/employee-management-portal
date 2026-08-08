package com.company.employeemanagement.service;

import com.company.employeemanagement.dto.request.CreateEmployeeRequest;
import com.company.employeemanagement.dto.request.UpdateEmployeeRequest;
import com.company.employeemanagement.dto.response.EmployeeResponse;
import com.company.employeemanagement.dto.response.PageResponse;
import com.company.employeemanagement.entity.Department;
import com.company.employeemanagement.entity.Employee;
import com.company.employeemanagement.entity.enums.EmployeeStatus;
import com.company.employeemanagement.exception.DuplicateResourceException;
import com.company.employeemanagement.exception.ResourceNotFoundException;
import com.company.employeemanagement.mapper.EmployeeMapper;
import com.company.employeemanagement.repository.DepartmentRepository;
import com.company.employeemanagement.repository.EmployeeRepository;
import com.company.employeemanagement.repository.UserRepository;
import com.company.employeemanagement.service.impl.EmployeeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link EmployeeServiceImpl}.
 *
 * @author Employee Management Portal Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmployeeServiceImpl")
class EmployeeServiceTest {

    @Mock private EmployeeRepository   employeeRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private UserRepository       userRepository;
    @Mock private EmployeeMapper       employeeMapper;

    private EmployeeServiceImpl employeeService;

    @BeforeEach
    void setUp() {
        employeeService = new EmployeeServiceImpl(
                employeeRepository, departmentRepository, userRepository, employeeMapper);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    private Department buildDepartment(final UUID id) {
        Department dept = new Department();
        dept.setName("Engineering");
        dept.setCode("ENG");
        return dept;
    }

    private Employee buildEmployee(final UUID id, final Department dept) {
        Employee emp = Employee.builder()
                .employeeCode("EMP-001")
                .department(dept)
                .jobTitle("Software Engineer")
                .dateOfJoining(LocalDate.of(2024, 1, 15))
                .salary(new BigDecimal("75000.00"))
                .status(EmployeeStatus.ACTIVE)
                .build();
        return emp;
    }

    private EmployeeResponse buildEmployeeResponse(final UUID id, final UUID deptId) {
        return new EmployeeResponse(
                id, "EMP-001", deptId, "Engineering",
                null, null, null, null,
                "Software Engineer", null, null,
                LocalDate.of(2024, 1, 15),
                new BigDecimal("75000.00"), EmployeeStatus.ACTIVE,
                LocalDateTime.now(), LocalDateTime.now()
        );
    }

    // ──────────────────────────────────────────────────────────────────────────
    // findAll()
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findAll()")
    class FindAll {

        @Test
        @DisplayName("returns all employees when no keyword supplied")
        void returnsAllWithoutKeyword() {
            UUID deptId = UUID.randomUUID();
            UUID empId  = UUID.randomUUID();
            Department dept = buildDepartment(deptId);
            Employee emp = buildEmployee(empId, dept);
            EmployeeResponse response = buildEmployeeResponse(empId, deptId);

            Pageable pageable = PageRequest.of(0, 20);
            Page<Employee> page = new PageImpl<>(List.of(emp), pageable, 1);

            when(employeeRepository.findAll(pageable)).thenReturn(page);
            when(employeeMapper.toResponse(emp)).thenReturn(response);

            PageResponse<EmployeeResponse> result = employeeService.findAll(null, pageable);

            assertThat(result.content()).hasSize(1);
            assertThat(result.totalElements()).isEqualTo(1);
            assertThat(result.page()).isEqualTo(0);
        }

        @Test
        @DisplayName("delegates to searchByKeyword when keyword is non-blank")
        void delegatesToKeywordSearch() {
            UUID deptId = UUID.randomUUID();
            UUID empId  = UUID.randomUUID();
            Department dept = buildDepartment(deptId);
            Employee emp = buildEmployee(empId, dept);
            EmployeeResponse response = buildEmployeeResponse(empId, deptId);

            Pageable pageable = PageRequest.of(0, 20);
            Page<Employee> page = new PageImpl<>(List.of(emp), pageable, 1);

            when(employeeRepository.searchByKeyword("engineer", pageable)).thenReturn(page);
            when(employeeMapper.toResponse(emp)).thenReturn(response);

            PageResponse<EmployeeResponse> result = employeeService.findAll("engineer", pageable);

            assertThat(result.content()).hasSize(1);
            verify(employeeRepository).searchByKeyword("engineer", pageable);
            verify(employeeRepository, never()).findAll(any(Pageable.class));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // findById()
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("returns employee DTO when found")
        void returnsEmployeeWhenFound() {
            UUID empId  = UUID.randomUUID();
            UUID deptId = UUID.randomUUID();
            Department dept = buildDepartment(deptId);
            Employee emp = buildEmployee(empId, dept);
            EmployeeResponse response = buildEmployeeResponse(empId, deptId);

            when(employeeRepository.findById(empId)).thenReturn(Optional.of(emp));
            when(employeeMapper.toResponse(emp)).thenReturn(response);

            EmployeeResponse result = employeeService.findById(empId);

            assertThat(result.id()).isEqualTo(empId);
            assertThat(result.employeeCode()).isEqualTo("EMP-001");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when employee does not exist")
        void throwsWhenNotFound() {
            UUID missingId = UUID.randomUUID();
            when(employeeRepository.findById(missingId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> employeeService.findById(missingId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Employee");
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // create()
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("saves and returns new employee DTO on success")
        void savesNewEmployee() {
            UUID deptId = UUID.randomUUID();
            Department dept = buildDepartment(deptId);
            UUID empId = UUID.randomUUID();
            Employee emp = buildEmployee(empId, dept);
            EmployeeResponse response = buildEmployeeResponse(empId, deptId);

            CreateEmployeeRequest request = new CreateEmployeeRequest(
                    null, "EMP-001", deptId, "Software Engineer",
                    null, null, LocalDate.of(2024, 1, 15),
                    new BigDecimal("75000.00"), EmployeeStatus.ACTIVE
            );

            when(employeeRepository.existsByEmployeeCode("EMP-001")).thenReturn(false);
            when(departmentRepository.findById(deptId)).thenReturn(Optional.of(dept));
            when(employeeRepository.save(any(Employee.class))).thenReturn(emp);
            when(employeeMapper.toResponse(emp)).thenReturn(response);

            EmployeeResponse result = employeeService.create(request);

            assertThat(result.employeeCode()).isEqualTo("EMP-001");
            verify(employeeRepository).save(any(Employee.class));
        }

        @Test
        @DisplayName("throws DuplicateResourceException when employee code is taken")
        void throwsOnDuplicateCode() {
            UUID deptId = UUID.randomUUID();
            CreateEmployeeRequest request = new CreateEmployeeRequest(
                    null, "EMP-001", deptId, "Engineer",
                    null, null, LocalDate.now(), BigDecimal.ZERO, EmployeeStatus.ACTIVE
            );

            when(employeeRepository.existsByEmployeeCode("EMP-001")).thenReturn(true);

            assertThatThrownBy(() -> employeeService.create(request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("EMP-001");

            verify(employeeRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when department does not exist")
        void throwsWhenDepartmentMissing() {
            UUID deptId = UUID.randomUUID();
            CreateEmployeeRequest request = new CreateEmployeeRequest(
                    null, "EMP-002", deptId, "Engineer",
                    null, null, LocalDate.now(), BigDecimal.ZERO, EmployeeStatus.ACTIVE
            );

            when(employeeRepository.existsByEmployeeCode("EMP-002")).thenReturn(false);
            when(departmentRepository.findById(deptId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> employeeService.create(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Department");
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // update()
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("updates and returns employee DTO on success")
        void updatesEmployee() {
            UUID empId  = UUID.randomUUID();
            UUID deptId = UUID.randomUUID();
            Department dept = buildDepartment(deptId);
            Employee emp = buildEmployee(empId, dept);
            EmployeeResponse response = buildEmployeeResponse(empId, deptId);

            UpdateEmployeeRequest request = new UpdateEmployeeRequest(
                    deptId, "Principal Engineer", null, null,
                    LocalDate.of(2024, 1, 15), new BigDecimal("90000.00"), EmployeeStatus.ACTIVE
            );

            when(employeeRepository.findById(empId)).thenReturn(Optional.of(emp));
            when(departmentRepository.findById(deptId)).thenReturn(Optional.of(dept));
            when(employeeRepository.save(emp)).thenReturn(emp);
            when(employeeMapper.toResponse(emp)).thenReturn(response);

            EmployeeResponse result = employeeService.update(empId, request);

            assertThat(result).isNotNull();
            verify(employeeRepository).save(emp);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // delete()
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("deletes employee by ID when it exists")
        void deletesWhenExists() {
            UUID empId = UUID.randomUUID();
            when(employeeRepository.existsById(empId)).thenReturn(true);

            employeeService.delete(empId);

            verify(employeeRepository).deleteById(empId);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when employee does not exist")
        void throwsWhenNotFound() {
            UUID missingId = UUID.randomUUID();
            when(employeeRepository.existsById(missingId)).thenReturn(false);

            assertThatThrownBy(() -> employeeService.delete(missingId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Employee");

            verify(employeeRepository, never()).deleteById(any());
        }
    }
}
