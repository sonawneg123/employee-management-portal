package com.company.employeemanagement.config;

import com.company.employeemanagement.entity.Department;
import com.company.employeemanagement.entity.Employee;
import com.company.employeemanagement.entity.Role;
import com.company.employeemanagement.entity.User;
import com.company.employeemanagement.entity.enums.EmployeeStatus;
import com.company.employeemanagement.repository.DepartmentRepository;
import com.company.employeemanagement.repository.EmployeeRepository;
import com.company.employeemanagement.repository.RoleRepository;
import com.company.employeemanagement.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Idempotent seed component that ensures a demo {@code employee@company.com}
 * user account and its linked {@link Employee} record exist on every startup.
 *
 * <p>This is the reliable alternative to a Flyway SQL migration for the
 * employee seed because it uses the live {@link PasswordEncoder} bean to hash
 * the password — no pre-computed BCrypt hash required.
 *
 * <p>Safe to run multiple times: every step is guarded by an existence check.
 *
 * @author Employee Management Portal Team
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private static final String EMPLOYEE_EMAIL    = "employee@company.com";
    private static final String EMPLOYEE_PASSWORD = "Employee@1234!";
    private static final String EMPLOYEE_CODE     = "EMP-0001";
    private static final String ROLE_EMPLOYEE     = "ROLE_EMPLOYEE";
    private static final String DEFAULT_DEPT_NAME = "General";
    private static final String DEFAULT_DEPT_CODE = "GEN";

    private final UserRepository       userRepository;
    private final RoleRepository       roleRepository;
    private final EmployeeRepository   employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder      passwordEncoder;

    public DataInitializer(final UserRepository userRepository,
                           final RoleRepository roleRepository,
                           final EmployeeRepository employeeRepository,
                           final DepartmentRepository departmentRepository,
                           final PasswordEncoder passwordEncoder) {
        this.userRepository       = userRepository;
        this.roleRepository       = roleRepository;
        this.employeeRepository   = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder      = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(final String... args) {
        seedEmployeeAccount();
    }

    private void seedEmployeeAccount() {
        // ── 1. Resolve or create the User ────────────────────────────────────
        User user = userRepository.findByEmail(EMPLOYEE_EMAIL).orElseGet(() -> {
            log.info("DataInitializer: creating seed user {}", EMPLOYEE_EMAIL);
            Role employeeRole = roleRepository.findByName(ROLE_EMPLOYEE)
                    .orElseThrow(() -> new IllegalStateException(
                            "Role " + ROLE_EMPLOYEE + " not found — run Flyway migrations first"));

            // Use a mutable HashSet — Set.of() produces an immutable set that causes
            // UnsupportedOperationException when AdminService tries to change the role.
            Set<Role> roles = new HashSet<>();
            roles.add(employeeRole);
            User newUser = User.builder()
                    .email(EMPLOYEE_EMAIL)
                    .passwordHash(passwordEncoder.encode(EMPLOYEE_PASSWORD))
                    .firstName("Demo")
                    .lastName("Employee")
                    .isEnabled(true)
                    .isLocked(false)
                    .roles(roles)
                    .build();
            return userRepository.save(newUser);
        });

        // ── 2. Resolve or create a Department ────────────────────────────────
        Department department = departmentRepository.findByCode(DEFAULT_DEPT_CODE)
                .orElseGet(() -> {
                    List<Department> all = departmentRepository.findAll();
                    if (!all.isEmpty()) {
                        return all.get(0);
                    }
                    log.info("DataInitializer: creating default department '{}'", DEFAULT_DEPT_NAME);
                    Department dept = Department.builder()
                            .name(DEFAULT_DEPT_NAME)
                            .code(DEFAULT_DEPT_CODE)
                            .build();
                    return departmentRepository.save(dept);
                });

        // ── 3. Resolve or create the Employee record ──────────────────────────
        boolean employeeExists = employeeRepository.findByUserId(user.getId()).isPresent()
                || employeeRepository.existsByEmployeeCode(EMPLOYEE_CODE);
        if (!employeeExists) {
            log.info("DataInitializer: creating Employee record for {}", EMPLOYEE_EMAIL);
            Employee employee = Employee.builder()
                    .user(user)
                    .employeeCode(EMPLOYEE_CODE)
                    .department(department)
                    .jobTitle("Software Engineer")
                    .dateOfJoining(LocalDate.of(2024, 1, 1))
                    .salary(BigDecimal.ZERO)
                    .status(EmployeeStatus.ACTIVE)
                    .build();
            employeeRepository.save(employee);
        }
    }
}
