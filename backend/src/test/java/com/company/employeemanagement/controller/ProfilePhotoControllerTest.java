package com.company.employeemanagement.controller;

import com.company.employeemanagement.entity.Employee;
import com.company.employeemanagement.entity.User;
import com.company.employeemanagement.entity.enums.EmployeeStatus;
import com.company.employeemanagement.exception.ResourceNotFoundException;
import com.company.employeemanagement.repository.DepartmentRepository;
import com.company.employeemanagement.repository.EmployeeRepository;
import com.company.employeemanagement.repository.UserRepository;
import com.company.employeemanagement.security.SecurityUtils;
import com.company.employeemanagement.service.FileStorageService;
import com.company.employeemanagement.service.ProfilePhotoValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for profile photo endpoints in {@link ProfileController}.
 *
 * <p>These tests exercise the controller logic in isolation using Mockito mocks,
 * without loading the Spring application context.
 *
 * @author Employee Management Portal Team
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ProfileController — photo endpoints")
class ProfilePhotoControllerTest {

    @Mock private SecurityUtils                securityUtils;
    @Mock private UserRepository               userRepository;
    @Mock private EmployeeRepository           employeeRepository;
    @Mock private DepartmentRepository         departmentRepository;
    @Mock private FileStorageService           fileStorageService;
    @Mock private ProfilePhotoValidationService photoValidationService;

    private ProfileController controller;

    @BeforeEach
    void setUp() {
        controller = new ProfileController(
                securityUtils, userRepository, employeeRepository,
                departmentRepository, fileStorageService, photoValidationService);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private User buildUser(final UUID userId) {
        com.company.employeemanagement.entity.Role role =
                new com.company.employeemanagement.entity.Role();
        role.setName("ROLE_EMPLOYEE");

        User user = User.builder()
                .firstName("Jane")
                .lastName("Doe")
                .email("jane@example.com")
                .passwordHash("hash")
                .build();
        user.setId(userId);
        user.setRoles(Set.of(role));
        return user;
    }

    private Employee buildEmployee(final UUID empId, final UUID userId) {
        com.company.employeemanagement.entity.Department dept =
                new com.company.employeemanagement.entity.Department();
        dept.setName("Engineering");
        dept.setCode("ENG");
        dept.setId(UUID.randomUUID());

        Employee emp = Employee.builder()
                .employeeCode("EMP-001")
                .department(dept)
                .jobTitle("Engineer")
                .dateOfJoining(LocalDate.of(2024, 1, 1))
                .salary(BigDecimal.valueOf(75000))
                .status(EmployeeStatus.ACTIVE)
                .build();
        emp.setId(empId);
        return emp;
    }

    private MockMultipartFile validJpeg() {
        return new MockMultipartFile(
                "photo", "avatar.jpg", "image/jpeg",
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0});
    }

    // ── uploadPhoto ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("uploadPhoto()")
    class UploadPhoto {

        @Test
        @DisplayName("valid JPEG upload succeeds: storage key saved and photo URL returned")
        void uploadPhoto_validJpeg_succeeds() throws Exception {
            UUID userId = UUID.randomUUID();
            UUID empId  = UUID.randomUUID();
            User user = buildUser(userId);
            Employee employee = buildEmployee(empId, userId);
            String storageKey = "profiles/" + empId + "/abc.jpg";

            when(securityUtils.getCurrentUsername()).thenReturn("jane@example.com");
            when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
            when(employeeRepository.findByUserId(userId)).thenReturn(Optional.of(employee));
            when(fileStorageService.storeProfilePhoto(any(MultipartFile.class), eq(empId)))
                    .thenReturn(storageKey);
            when(employeeRepository.save(any(Employee.class))).thenReturn(employee);

            MockMultipartFile photo = validJpeg();
            var response = controller.uploadPhoto(photo);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isNotNull();
            // profilePhotoUrl should now be set
            assertThat(response.getBody().profilePhotoUrl()).isEqualTo("/api/profile/photo");
            verify(fileStorageService).storeProfilePhoto(photo, empId);
            verify(employeeRepository).save(any(Employee.class));
        }

        @Test
        @DisplayName("invalid MIME type causes IllegalArgumentException (400)")
        void uploadPhoto_invalidMime_returns400() throws Exception {
            UUID userId = UUID.randomUUID();
            UUID empId  = UUID.randomUUID();
            User user = buildUser(userId);
            Employee employee = buildEmployee(empId, userId);

            when(securityUtils.getCurrentUsername()).thenReturn("jane@example.com");
            when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
            when(employeeRepository.findByUserId(userId)).thenReturn(Optional.of(employee));

            // Simulate validation failure
            MockMultipartFile badFile = new MockMultipartFile(
                    "photo", "document.pdf", "application/pdf", new byte[]{1, 2, 3});
            doThrow(new IllegalArgumentException("Unsupported image type"))
                    .when(photoValidationService).validate(badFile);

            assertThatThrownBy(() -> controller.uploadPhoto(badFile))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unsupported image type");

            // Storage must not be touched
            verify(fileStorageService, never()).storeProfilePhoto(any(), any());
        }

        @Test
        @DisplayName("oversized file causes IllegalArgumentException (400)")
        void uploadPhoto_oversized_returns400() throws Exception {
            UUID userId = UUID.randomUUID();
            UUID empId  = UUID.randomUUID();
            User user = buildUser(userId);
            Employee employee = buildEmployee(empId, userId);

            when(securityUtils.getCurrentUsername()).thenReturn("jane@example.com");
            when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
            when(employeeRepository.findByUserId(userId)).thenReturn(Optional.of(employee));

            MockMultipartFile bigFile = new MockMultipartFile(
                    "photo", "big.jpg", "image/jpeg", new byte[6 * 1024 * 1024]);
            doThrow(new IllegalArgumentException("Image size exceeds 5 MB limit."))
                    .when(photoValidationService).validate(bigFile);

            assertThatThrownBy(() -> controller.uploadPhoto(bigFile))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("5 MB");

            verify(fileStorageService, never()).storeProfilePhoto(any(), any());
        }
    }

    // ── deletePhoto ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deletePhoto()")
    class DeletePhoto {

        @Test
        @DisplayName("returns 404 when employee has no photo to delete")
        void deletePhoto_noPhoto_returns404() {
            UUID userId = UUID.randomUUID();
            UUID empId  = UUID.randomUUID();
            User user = buildUser(userId);
            Employee employee = buildEmployee(empId, userId);
            // No photo stored
            employee.setProfilePhotoStorageKey(null);

            when(securityUtils.getCurrentUsername()).thenReturn("jane@example.com");
            when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
            when(employeeRepository.findByUserId(userId)).thenReturn(Optional.of(employee));

            assertThatThrownBy(() -> controller.deletePhoto())
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Profile photo");
        }

        @Test
        @DisplayName("deletes photo successfully and returns 204")
        void deletePhoto_withPhoto_returns204() {
            UUID userId = UUID.randomUUID();
            UUID empId  = UUID.randomUUID();
            User user = buildUser(userId);
            Employee employee = buildEmployee(empId, userId);
            employee.setProfilePhotoStorageKey("profiles/" + empId + "/abc.jpg");

            when(securityUtils.getCurrentUsername()).thenReturn("jane@example.com");
            when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
            when(employeeRepository.findByUserId(userId)).thenReturn(Optional.of(employee));
            when(employeeRepository.save(any(Employee.class))).thenReturn(employee);

            var response = controller.deletePhoto();

            assertThat(response.getStatusCode().value()).isEqualTo(204);
            verify(fileStorageService).delete("profiles/" + empId + "/abc.jpg");
        }
    }

    // ── getPhoto ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getPhoto()")
    class GetPhoto {

        @Test
        @DisplayName("returns 404 when employee has no photo uploaded")
        void getPhoto_noPhoto_returns404() throws Exception {
            UUID userId = UUID.randomUUID();
            UUID empId  = UUID.randomUUID();
            User user = buildUser(userId);
            Employee employee = buildEmployee(empId, userId);
            employee.setProfilePhotoStorageKey(null);

            when(securityUtils.getCurrentUsername()).thenReturn("jane@example.com");
            when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
            when(employeeRepository.findByUserId(userId)).thenReturn(Optional.of(employee));

            assertThatThrownBy(() -> controller.getPhoto())
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Profile photo");
        }

        @Test
        @DisplayName("streams photo with correct content type when photo exists")
        void getPhoto_withPhoto_returns200() throws Exception {
            UUID userId = UUID.randomUUID();
            UUID empId  = UUID.randomUUID();
            User user = buildUser(userId);
            Employee employee = buildEmployee(empId, userId);
            String storageKey = "profiles/" + empId + "/abc.jpg";
            employee.setProfilePhotoStorageKey(storageKey);
            employee.setProfilePhotoStoredName("abc.jpg");
            employee.setProfilePhotoMimeType("image/jpeg");

            when(securityUtils.getCurrentUsername()).thenReturn("jane@example.com");
            when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
            when(employeeRepository.findByUserId(userId)).thenReturn(Optional.of(employee));
            when(fileStorageService.openForRead(storageKey))
                    .thenReturn(new ByteArrayInputStream(new byte[]{1, 2, 3}));

            var response = controller.getPhoto();

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getHeaders().getContentType())
                    .hasToString("image/jpeg");
            verify(fileStorageService).openForRead(storageKey);
        }
    }
}
