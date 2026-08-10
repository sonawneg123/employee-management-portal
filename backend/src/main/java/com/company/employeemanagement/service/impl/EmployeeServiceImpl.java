package com.company.employeemanagement.service.impl;

import com.company.employeemanagement.dto.request.CreateEmployeeRequest;
import com.company.employeemanagement.dto.request.UpdateEmployeeRequest;
import com.company.employeemanagement.dto.response.EmployeeResponse;
import com.company.employeemanagement.dto.response.PageResponse;
import com.company.employeemanagement.entity.Department;
import com.company.employeemanagement.entity.Employee;
import com.company.employeemanagement.entity.User;
import com.company.employeemanagement.entity.enums.EmployeeStatus;
import com.company.employeemanagement.exception.AccessDeniedException;
import com.company.employeemanagement.exception.DuplicateResourceException;
import com.company.employeemanagement.exception.ResourceNotFoundException;
import com.company.employeemanagement.mapper.EmployeeMapper;
import com.company.employeemanagement.repository.DepartmentRepository;
import com.company.employeemanagement.repository.EmployeeRepository;
import com.company.employeemanagement.repository.UserRepository;
import com.company.employeemanagement.security.SecurityUtils;
import com.company.employeemanagement.service.EmployeeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implementation of {@link EmployeeService} providing full CRUD operations
 * with pagination, keyword search, and relationship resolution.
 *
 * <p>All write operations are wrapped in transactions. Read operations use
 * {@code readOnly = true} transactions to allow the JPA provider to apply
 * optimisations such as skipping dirty-checking.
 *
 * @author Employee Management Portal Team
 */
@Service
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final EmployeeMapper employeeMapper;
    private final SecurityUtils securityUtils;

    /**
     * Constructs the service with all required dependencies.
     *
     * @param employeeRepository   repository for employee persistence
     * @param departmentRepository repository for department lookups
     * @param userRepository       repository for optional user account lookups
     * @param employeeMapper       MapStruct mapper for entity-to-DTO conversion
     * @param securityUtils        helper for current-principal inspection
     */
    public EmployeeServiceImpl(final EmployeeRepository employeeRepository,
                                 final DepartmentRepository departmentRepository,
                                 final UserRepository userRepository,
                                 final EmployeeMapper employeeMapper,
                                 final SecurityUtils securityUtils) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.employeeMapper = employeeMapper;
        this.securityUtils = securityUtils;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Uses a two-step query strategy to prevent N+1 selects:
     * <ol>
     *   <li>A scalar ID query fetches the correct page of primary keys with
     *       database-level pagination ({@code LIMIT}/{@code OFFSET}).</li>
     *   <li>A single {@code JOIN FETCH} query loads all employee entities
     *       (with their {@code department} and {@code user} associations)
     *       for those IDs in one round-trip.</li>
     * </ol>
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<EmployeeResponse> findAll(final String keyword,
                                                   final UUID departmentId,
                                                   final EmployeeStatus status,
                                                   final Pageable pageable) {
        // Step 1 — paginated ID query with optional filters
        final String effectiveKeyword = StringUtils.hasText(keyword) ? keyword : null;
        final Page<UUID> idPage = employeeRepository.findIdsByFilters(
                effectiveKeyword, departmentId, status, pageable);

        if (idPage.isEmpty()) {
            return PageResponse.from(new PageImpl<>(List.of(), pageable, idPage.getTotalElements()));
        }

        // Step 2 — batch-fetch full entities with associations in one query
        final List<UUID> ids = idPage.getContent();
        final Map<UUID, Employee> byId = employeeRepository
                .findAllWithAssociationsByIds(ids)
                .stream()
                .collect(Collectors.toMap(Employee::getId, Function.identity()));

        // Reconstruct the page in the original paginated order
        final List<EmployeeResponse> content = ids.stream()
                .filter(byId::containsKey)
                .map(id -> employeeMapper.toResponse(byId.get(id)))
                .collect(Collectors.toList());

        Page<EmployeeResponse> page = new PageImpl<>(content, pageable, idPage.getTotalElements());
        return PageResponse.from(page);
    }

    /**
     * {@inheritDoc}
     *
     * <p>An EMPLOYEE principal can only retrieve their own employee record.
     * Attempting to retrieve another employee's record yields a 403.
     */
    @Override
    @Transactional(readOnly = true)
    public EmployeeResponse findById(final UUID id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", id));

        if (securityUtils.hasRole("ROLE_EMPLOYEE") && !securityUtils.isPrivileged()) {
            Employee ownEmployee = securityUtils.getCurrentEmployee()
                    .orElseThrow(() -> new AccessDeniedException(
                            "No employee record is linked to your account."));
            if (!ownEmployee.getId().equals(employee.getId())) {
                throw new AccessDeniedException(
                        "You may only access your own employee record.");
            }
        }

        return employeeMapper.toResponse(employee);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The optional {@code userId} field, when present, is resolved to a
     * {@link User} entity before persisting. If the user ID is supplied but
     * the user does not exist, a {@link ResourceNotFoundException} is thrown.
     */
    @Override
    @Transactional
    public EmployeeResponse create(final CreateEmployeeRequest request) {
        if (employeeRepository.existsByEmployeeCode(request.employeeCode())) {
            throw new DuplicateResourceException("Employee", "employeeCode", request.employeeCode());
        }

        Department department = departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Department", request.departmentId()));

        User user = null;
        if (request.userId() != null) {
            user = userRepository.findById(request.userId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", request.userId()));
        }

        Employee employee = Employee.builder()
                .employeeCode(request.employeeCode())
                .department(department)
                .user(user)
                .jobTitle(request.jobTitle())
                .phone(request.phone())
                .address(request.address())
                .dateOfJoining(request.dateOfJoining())
                .salary(request.salary())
                .status(request.status())
                .build();

        Employee saved = employeeRepository.save(employee);
        return employeeMapper.toResponse(saved);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public EmployeeResponse update(final UUID id, final UpdateEmployeeRequest request) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", id));

        Department department = departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Department", request.departmentId()));

        employee.setDepartment(department);
        employee.setJobTitle(request.jobTitle());
        employee.setPhone(request.phone());
        employee.setAddress(request.address());
        employee.setDateOfJoining(request.dateOfJoining());
        employee.setSalary(request.salary());
        employee.setStatus(request.status());

        Employee updated = employeeRepository.save(employee);
        return employeeMapper.toResponse(updated);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void delete(final UUID id) {
        if (!employeeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Employee", id);
        }
        employeeRepository.deleteById(id);
    }
}
