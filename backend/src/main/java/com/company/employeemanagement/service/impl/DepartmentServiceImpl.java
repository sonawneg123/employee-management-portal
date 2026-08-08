package com.company.employeemanagement.service.impl;

import com.company.employeemanagement.dto.request.CreateDepartmentRequest;
import com.company.employeemanagement.dto.request.UpdateDepartmentRequest;
import com.company.employeemanagement.dto.response.DepartmentResponse;
import com.company.employeemanagement.dto.response.PageResponse;
import com.company.employeemanagement.entity.Department;
import com.company.employeemanagement.exception.DuplicateResourceException;
import com.company.employeemanagement.exception.ResourceNotFoundException;
import com.company.employeemanagement.mapper.DepartmentMapper;
import com.company.employeemanagement.repository.DepartmentRepository;
import com.company.employeemanagement.service.DepartmentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

/**
 * Implementation of {@link DepartmentService} providing full CRUD operations
 * with optional keyword search and pagination.
 *
 * <p>All write operations are transactional. Reads use {@code readOnly = true}
 * to enable Hibernate dirty-checking optimisations.
 *
 * @author Employee Management Portal Team
 */
@Service
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final DepartmentMapper departmentMapper;

    /**
     * Constructs the service with required dependencies.
     *
     * @param departmentRepository repository for department persistence
     * @param departmentMapper     MapStruct mapper for entity-to-DTO conversion
     */
    public DepartmentServiceImpl(final DepartmentRepository departmentRepository,
                                  final DepartmentMapper departmentMapper) {
        this.departmentRepository = departmentRepository;
        this.departmentMapper = departmentMapper;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns all departments sorted by name ascending for stable dropdown ordering.
     */
    @Override
    @Transactional(readOnly = true)
    public List<DepartmentResponse> findAll() {
        return departmentRepository.findAll(Sort.by("name").ascending())
                .stream()
                .map(departmentMapper::toResponse)
                .toList();
    }

    /**
     * {@inheritDoc}
     *
     * <p>When {@code keyword} is non-blank, delegates to a case-insensitive LIKE
     * search across name and code. Otherwise returns all departments.
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<DepartmentResponse> findAllPaged(final String keyword,
                                                          final Pageable pageable) {
        Page<DepartmentResponse> page;
        if (StringUtils.hasText(keyword)) {
            page = departmentRepository.searchByKeyword(keyword, pageable)
                    .map(departmentMapper::toResponse);
        } else {
            page = departmentRepository.findAll(pageable)
                    .map(departmentMapper::toResponse);
        }
        return PageResponse.from(page);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public DepartmentResponse findById(final UUID id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", id));
        return departmentMapper.toResponse(department);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Guards against duplicate department codes before persisting.
     */
    @Override
    @Transactional
    public DepartmentResponse create(final CreateDepartmentRequest request) {
        if (departmentRepository.existsByCode(request.code())) {
            throw new DuplicateResourceException("Department", "code", request.code());
        }

        Department department = Department.builder()
                .name(request.name())
                .code(request.code())
                .build();

        Department saved = departmentRepository.save(department);
        return departmentMapper.toResponse(saved);
    }

    /**
     * {@inheritDoc}
     *
     * <p>If the code is being changed, verifies that the new code is not
     * already in use by a different department.
     */
    @Override
    @Transactional
    public DepartmentResponse update(final UUID id, final UpdateDepartmentRequest request) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", id));

        // Guard: only reject on code conflict if the code is actually changing
        if (!department.getCode().equalsIgnoreCase(request.code())
                && departmentRepository.existsByCode(request.code())) {
            throw new DuplicateResourceException("Department", "code", request.code());
        }

        department.setName(request.name());
        department.setCode(request.code());

        Department updated = departmentRepository.save(department);
        return departmentMapper.toResponse(updated);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void delete(final UUID id) {
        if (!departmentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Department", id);
        }
        departmentRepository.deleteById(id);
    }
}
