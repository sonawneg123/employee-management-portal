package com.company.employeemanagement.entity;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.company.employeemanagement.entity.enums.LeaveStatus;
import com.company.employeemanagement.entity.enums.LeaveType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a leave request submitted by an {@link Employee}.
 *
 * <p>The request goes through a lifecycle: {@code PENDING} → {@code APPROVED}
 * or {@code REJECTED}, and may be {@code CANCELLED} by the employee before
 * review.
 *
 * @author Employee Management Portal Team
 */
@Entity
@Table(name = "leave_requests")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveRequest extends BaseEntity {

    /**
     * The employee who submitted this leave request.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    /**
     * Category of leave being requested.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "leave_type", nullable = false, length = 20)
    private LeaveType leaveType;

    /**
     * Inclusive start date of the requested leave period.
     */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /**
     * Inclusive end date of the requested leave period.
     */
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /**
     * Optional reason or justification provided by the employee.
     */
    @Column(name = "reason", length = 500)
    private String reason;

    /**
     * Current approval status of the leave request.
     * Defaults to {@link LeaveStatus#PENDING}.
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private LeaveStatus status = LeaveStatus.PENDING;

    /**
     * UUID of the HR or manager user who reviewed this request.
     * Null while the request is still pending.
     */
    @JdbcTypeCode(SqlTypes.CHAR)
@Column(name = "reviewed_by")
private UUID reviewedBy;
    /**
     * Timestamp when the review decision was made.
     * Null while the request is still pending.
     */
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
}
