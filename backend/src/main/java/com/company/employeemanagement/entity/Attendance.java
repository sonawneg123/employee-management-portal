package com.company.employeemanagement.entity;

import com.company.employeemanagement.entity.enums.AttendanceStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Records a single day's attendance entry for an {@link Employee}.
 *
 * <p>A composite unique constraint ensures that only one attendance record
 * can exist per employee per date ({@code uq_attendance_emp_date}).
 *
 * @author Employee Management Portal Team
 */
@Entity
@Table(
        name = "attendance",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_attendance_emp_date",
                columnNames = {"employee_id", "attendance_date"}
        )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Attendance extends BaseEntity {

    /**
     * The employee whose attendance is being recorded.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    /**
     * The calendar date for this attendance record.
     */
    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    /**
     * The time the employee checked in. May be null for absent or leave days.
     */
    @Column(name = "check_in_time")
    private LocalTime checkInTime;

    /**
     * The time the employee checked out. May be null if not yet checked out.
     */
    @Column(name = "check_out_time")
    private LocalTime checkOutTime;

    /**
     * The attendance status for this record.
     * Defaults to {@link AttendanceStatus#PRESENT}.
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AttendanceStatus status = AttendanceStatus.PRESENT;

    /**
     * Optional free-text notes about this attendance record
     * (e.g., "Left early due to medical appointment").
     */
    @Column(name = "notes", length = 255)
    private String notes;
}
