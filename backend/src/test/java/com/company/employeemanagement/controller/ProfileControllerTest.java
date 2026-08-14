package com.company.employeemanagement.controller;

import com.company.employeemanagement.dto.request.UpdateProfileRequest;
import com.company.employeemanagement.dto.response.ProfileResponse;
import com.company.employeemanagement.entity.enums.EmployeeStatus;
import com.company.employeemanagement.exception.AccessDeniedException;
import com.company.employeemanagement.exception.GlobalExceptionHandler;
import com.company.employeemanagement.exception.ResourceNotFoundException;
import com.company.employeemanagement.repository.DepartmentRepository;
import com.company.employeemanagement.repository.EmployeeRepository;
import com.company.employeemanagement.repository.UserRepository;
import com.company.employeemanagement.security.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for {@link ProfileController} using standalone MockMvc.
 *
 * <p>Security enforcement (JWT, @PreAuthorize) is tested in the integration
 * test suite. These tests focus on HTTP semantics, service delegation, and
 * correct response shape.
 *
 * <p>Three key scenarios are covered:
 * <ul>
 *   <li>Unauthenticated requests → 403 (standalone MockMvc returns 403 for
 *       AccessDeniedException when no authentication principal is available).</li>
 *   <li>Authenticated user fetches their own profile → 200 with correct data.</li>
 *   <li>Authenticated user updates personal info → 200 with updated data.</li>
 * </ul>
 *
 * @author Employee Management Portal Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProfileController")
class ProfileControllerTest {

    @Mock private SecurityUtils      securityUtils;
    @Mock private UserRepository     userRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private DepartmentRepository departmentRepository;

    @InjectMocks
    private ProfileController profileController;

    private MockMvc     mockMvc;
    private ObjectMapper objectMapper;

    // ── Fixed test identifiers ────────────────────────────────────────────────

    private static final UUID   USER_ID     = UUID.randomUUID();
    private static final UUID   EMPLOYEE_ID = UUID.randomUUID();
    private static final UUID   DEPT_ID     = UUID.randomUUID();
    private static final String EMAIL       = "john.doe@example.com";
    private static final String FIRST_NAME  = "John";
    private static final String LAST_NAME   = "Doe";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        mockMvc = MockMvcBuilders
                .standaloneSetup(profileController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Builds a sample {@link ProfileResponse} with populated employee fields.
     */
    private ProfileResponse buildProfileResponse(final String firstName, final String lastName) {
        return new ProfileResponse(
                USER_ID, EMAIL,
                firstName, lastName,
                "ROLE_EMPLOYEE",
                EMPLOYEE_ID, "EMP-0001",
                DEPT_ID, "Engineering",
                "Software Engineer",
                "+1-555-0100",
                "123 Main St",
                LocalDate.of(2024, 1, 15),
                BigDecimal.valueOf(75000),
                EmployeeStatus.ACTIVE);
    }

    /**
     * Builds a {@link com.company.employeemanagement.entity.User} stub sufficient
     * for the controller's {@code resolveCurrentUser()} path.
     */
    private com.company.employeemanagement.entity.User buildUserEntity(
            final String firstName, final String lastName) {
        com.company.employeemanagement.entity.User user =
                new com.company.employeemanagement.entity.User();
        user.setId(USER_ID);
        user.setEmail(EMAIL);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setRoles(java.util.Set.of());
        return user;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /profile
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /profile")
    class GetProfile {

        @Test
        @DisplayName("returns 200 with profile data for authenticated user")
        void returnsProfileForAuthenticatedUser() throws Exception {
            com.company.employeemanagement.entity.User userEntity =
                    buildUserEntity(FIRST_NAME, LAST_NAME);

            when(securityUtils.getCurrentUsername()).thenReturn(EMAIL);
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(userEntity));
            when(employeeRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
            // No employee record — controller returns profile without employee fields

            mockMvc.perform(get("/profile")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value(EMAIL))
                    .andExpect(jsonPath("$.firstName").value(FIRST_NAME))
                    .andExpect(jsonPath("$.lastName").value(LAST_NAME));
        }

        @Test
        @DisplayName("returns 403 when no authentication is present")
        void returns403WhenNotAuthenticated() throws Exception {
            when(securityUtils.getCurrentUsername()).thenReturn(null);

            mockMvc.perform(get("/profile")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("returns 404 when user is authenticated but not found in DB")
        void returns404WhenUserNotFound() throws Exception {
            when(securityUtils.getCurrentUsername()).thenReturn(EMAIL);
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            mockMvc.perform(get("/profile")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("includes employee fields when a linked Employee record exists")
        void includesEmployeeFieldsWhenEmployeeExists() throws Exception {
            com.company.employeemanagement.entity.User userEntity =
                    buildUserEntity(FIRST_NAME, LAST_NAME);

            com.company.employeemanagement.entity.Department dept =
                    com.company.employeemanagement.entity.Department.builder()
                            .name("Engineering")
                            .code("ENG")
                            .build();
            dept.setId(DEPT_ID);

            com.company.employeemanagement.entity.Employee emp =
                    com.company.employeemanagement.entity.Employee.builder()
                            .user(userEntity)
                            .firstName(FIRST_NAME)
                            .lastName(LAST_NAME)
                            .employeeCode("EMP-0001")
                            .department(dept)
                            .jobTitle("Software Engineer")
                            .dateOfJoining(LocalDate.of(2024, 1, 15))
                            .salary(BigDecimal.valueOf(75000))
                            .status(EmployeeStatus.ACTIVE)
                            .build();
            emp.setId(EMPLOYEE_ID);

            when(securityUtils.getCurrentUsername()).thenReturn(EMAIL);
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(userEntity));
            when(employeeRepository.findByUserId(USER_ID)).thenReturn(Optional.of(emp));

            mockMvc.perform(get("/profile")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.employeeCode").value("EMP-0001"))
                    .andExpect(jsonPath("$.departmentName").value("Engineering"))
                    .andExpect(jsonPath("$.jobTitle").value("Software Engineer"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUT /profile/personal
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("PUT /profile/personal")
    class UpdatePersonal {

        @Test
        @DisplayName("returns 200 with updated name after successful save")
        void returnsUpdatedProfileOnSuccess() throws Exception {
            com.company.employeemanagement.entity.User userEntity =
                    buildUserEntity(FIRST_NAME, LAST_NAME);

            when(securityUtils.getCurrentUsername()).thenReturn(EMAIL);
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(userEntity));
            when(userRepository.save(any())).thenAnswer(inv -> {
                com.company.employeemanagement.entity.User saved = inv.getArgument(0);
                return saved;
            });
            when(employeeRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            UpdateProfileRequest request = new UpdateProfileRequest("Jane", "Smith", "+1-555-9999", "456 Oak Ave");

            mockMvc.perform(put("/profile/personal")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName").value("Jane"))
                    .andExpect(jsonPath("$.lastName").value("Smith"));
        }

        @Test
        @DisplayName("also updates phone and address on linked Employee record")
        void updatesPhoneAndAddressOnEmployee() throws Exception {
            com.company.employeemanagement.entity.User userEntity =
                    buildUserEntity(FIRST_NAME, LAST_NAME);

            com.company.employeemanagement.entity.Department dept =
                    com.company.employeemanagement.entity.Department.builder()
                            .name("Engineering").code("ENG").build();
            dept.setId(DEPT_ID);

            com.company.employeemanagement.entity.Employee emp =
                    com.company.employeemanagement.entity.Employee.builder()
                            .user(userEntity)
                            .firstName(FIRST_NAME)
                            .lastName(LAST_NAME)
                            .employeeCode("EMP-0001")
                            .department(dept)
                            .jobTitle("Engineer")
                            .dateOfJoining(LocalDate.of(2024, 1, 15))
                            .salary(BigDecimal.valueOf(75000))
                            .status(EmployeeStatus.ACTIVE)
                            .build();
            emp.setId(EMPLOYEE_ID);

            when(securityUtils.getCurrentUsername()).thenReturn(EMAIL);
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(userEntity));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(employeeRepository.findByUserId(USER_ID)).thenReturn(Optional.of(emp));
            when(employeeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UpdateProfileRequest request =
                    new UpdateProfileRequest("John", "Doe", "+1-555-7777", "789 Elm St");

            mockMvc.perform(put("/profile/personal")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.phone").value("+1-555-7777"))
                    .andExpect(jsonPath("$.address").value("789 Elm St"));
        }

        @Test
        @DisplayName("returns 403 when no authentication context is present")
        void returns403WhenNotAuthenticated() throws Exception {
            when(securityUtils.getCurrentUsername()).thenReturn(null);

            UpdateProfileRequest request =
                    new UpdateProfileRequest("Jane", "Smith", null, null);

            mockMvc.perform(put("/profile/personal")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("returns 400 when firstName exceeds 100 characters")
        void returns400WhenFirstNameTooLong() throws Exception {
            String tooLong = "A".repeat(101);
            UpdateProfileRequest request = new UpdateProfileRequest(tooLong, "Smith", null, null);

            mockMvc.perform(put("/profile/personal")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("leaves name unchanged when firstName is null in request")
        void keepsNameUnchangedWhenFirstNameIsNull() throws Exception {
            com.company.employeemanagement.entity.User userEntity =
                    buildUserEntity(FIRST_NAME, LAST_NAME);

            when(securityUtils.getCurrentUsername()).thenReturn(EMAIL);
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(userEntity));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(employeeRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            // Only phone provided — firstName/lastName are null → should keep original
            UpdateProfileRequest request = new UpdateProfileRequest(null, null, "+1-555-1234", null);

            mockMvc.perform(put("/profile/personal")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName").value(FIRST_NAME))
                    .andExpect(jsonPath("$.lastName").value(LAST_NAME));
        }
    }
}
