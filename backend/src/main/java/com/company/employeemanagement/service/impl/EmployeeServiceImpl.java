package com.company.employeemanagement.service.impl;

import com.company.employeemanagement.dto.request.CreateEmployeeRequest;
import com.company.employeemanagement.dto.request.UpdateEmployeeRequest;
import com.company.employeemanagement.dto.response.EmployeeResponse;
import com.company.employeemanagement.dto.response.PageResponse;
import com.company.employeemanagement.entity.Department;
import com.company.employeemanagement.entity.Employee;
import com.company.employeemanagement.entity.User;
import com.company.employeemanagement.exception.DuplicateResourceException;
import com.company.employeemanagement.exception.ResourceNotFoundException;
import com.company.employeemanagement.mapper.EmployeeMapper;
import com.company.employeemanagement.repository.DepartmentRepository;
import com.company.employeemanagement.repository.EmployeeRepository;
import com.company.employeemanagement.repository.UserRepository;
import com.company.employeemanagement.service.EmployeeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

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

    /**
     * Constructs the service with all required dependencies.
     *
     * @param employeeRepository   repository for employee persistence
     * @param departmentRepository repository for department lookups
     * @param userRepository       repository for optional user account lookups
     * @param employeeMapper       MapStruct mapper for entity-to-DTO conversion
     */
    public EmployeeServiceImpl(final EmployeeRepository employeeRepository,
                                final DepartmentRepository departmentRepository,
                                final UserRepository userRepository,
                                final EmployeeMapper employeeMapper) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.employeeMapper = employeeMapper;
    }

    /**
     * {@inheritDoc}
     *
     * <p>When {@code keyword} is non-blank, delegates to
     * {@link com.company.employeemanagement.repository.EmployeeRepository#searchByKeyword}
     * for a case-insensitive LIKE search across name and job title. Otherwise
     * returns all employees.
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<EmployeeResponse> findAll(final String keyword, final Pageable pageable) {
        Page<EmployeeResponse> page;
        if (StringUtils.hasText(keyword)) {
            page = employeeRepository.searchByKeyword(keyword, pageable)
                    .map(employeeMapper::toResponse);
        } else {
            page = employeeRepository.findAll(pageable)
                    .map(employeeMapper::toResponse);
        }
        return PageResponse.from(page);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public EmployeeResponse findById(final UUID id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", id));
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
