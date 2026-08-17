package com.company.employeemanagement.entity;

import com.company.employeemanagement.entity.enums.EmployeeStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Core domain entity representing an employee of the company.
 *
 * <p>Relationships:
 * <ul>
 *   <li>{@link Department} — many employees belong to one department.</li>
 *   <li>{@link User} — optional one-to-one link; an employee record may
 *       exist before a user account is created.</li>
 *   <li>{@link LeaveRequest} — one employee may have many leave requests.</li>
 *   <li>{@link Attendance} — one employee may have many attendance records.</li>
 *   <li>{@link PerformanceReview} — one employee may have many reviews.</li>
 * </ul>
 *
 * @author Employee Management Portal Team
 */
@Entity
@Table(name = "employees")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Employee extends BaseEntity {

    /**
     * Optional link to the {@link User} account for this employee.
     * An employee record can exist without a portal user account.
     * Nullified (not deleted) when the user account is removed.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    /**
     * First name of the employee, stored directly on the record so that
     * HR-created employees (without a portal User account) still have a
     * displayable name. When a User is linked, the UI and API prefer
     * the User's name; this column is the fallback.
     */
    @Column(name = "first_name", length = 100)
    private String firstName;

    /**
     * Last name of the employee. See {@link #firstName}.
     */
    @Column(name = "last_name", length = 100)
    private String lastName;

    /**
     * Unique employee code assigned by HR (e.g., "EMP-0001").
     * Used as an alternative stable external identifier.
     */
    @Column(name = "employee_code", nullable = false, unique = true, length = 20)
    private String employeeCode;

    /**
     * Department to which this employee belongs. Required — every employee
     * must be in exactly one department.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    /**
     * The employee's job title (e.g., "Senior Software Engineer").
     */
    @Column(name = "job_title", nullable = false, length = 150)
    private String jobTitle;

    /**
     * Contact phone number. Optional.
     */
    @Column(name = "phone", length = 20)
    private String phone;

    /**
     * Physical or mailing address. Optional.
     */
    @Column(name = "address", length = 255)
    private String address;

    /**
     * The date on which the employee officially joined the company.
     */
    @Column(name = "date_of_joining", nullable = false)
    private LocalDate dateOfJoining;

    /**
     * Gross salary in the company's base currency. Defaults to zero until
     * explicitly set.
     */
    @Builder.Default
    @Column(name = "salary", nullable = false, precision = 15, scale = 2)
    private BigDecimal salary = BigDecimal.ZERO;

    /**
     * Current employment status. See {@link EmployeeStatus} for possible
     * values. Defaults to {@link EmployeeStatus#ACTIVE}.
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EmployeeStatus status = EmployeeStatus.ACTIVE;

    /**
     * All leave requests submitted by this employee.
     * Lazily loaded; cascade persist/merge to propagate lifecycle.
     */
    @Builder.Default
    @OneToMany(mappedBy = "employee", fetch = FetchType.LAZY,
               cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<LeaveRequest> leaveRequests = new ArrayList<>();

    /**
     * All attendance records for this employee.
     * Lazily loaded.
     */
    @Builder.Default
    @OneToMany(mappedBy = "employee", fetch = FetchType.LAZY,
               cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<Attendance> attendances = new ArrayList<>();

    /**
     * All performance reviews for this employee.
     * Lazily loaded.
     */
    @Builder.Default
    @OneToMany(mappedBy = "employee", fetch = FetchType.LAZY,
               cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<PerformanceReview> performanceReviews = new ArrayList<>();

    // ── Profile photo fields ───────────────────────────────────────────────────

    /** Original filename as supplied by the client during upload. */
    @Column(name = "profile_photo_original_name", length = 255)
    private String profilePhotoOriginalName;

    /** UUID-based stored filename on disk (prevents collisions and path guessing). */
    @Column(name = "profile_photo_stored_name", length = 255)
    private String profilePhotoStoredName;

    /** MIME type of the uploaded photo (e.g., {@code image/jpeg}). */
    @Column(name = "profile_photo_mime_type", length = 100)
    private String profilePhotoMimeType;

    /** File size in bytes at the time of upload. */
    @Column(name = "profile_photo_size_bytes")
    private Long profilePhotoSizeBytes;

    /**
     * Storage key used to retrieve or delete the file via {@link com.company.employeemanagement.service.FileStorageService}.
     * Format: {@code profiles/{employeeId}/{uuid}.{ext}}.
     */
    @Column(name = "profile_photo_storage_key", length = 500)
    private String profilePhotoStorageKey;

    /** Timestamp of the most recent successful photo upload. */
    @Column(name = "profile_photo_uploaded_at")
    private LocalDateTime profilePhotoUploadedAt;
}
