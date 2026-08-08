package com.company.employeemanagement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Abstract base entity providing common auditing fields for all domain entities.
 *
 * <p>Every persistent entity in the application must extend this class to
 * receive:
 * <ul>
 *   <li>A UUID primary key generated at the database level via
 *       {@link GenerationType#UUID}.</li>
 *   <li>Automatic timestamps managed by Spring Data JPA Auditing:
 *       {@code createdAt} and {@code updatedAt}.</li>
 *   <li>Auditor tracking: {@code createdBy} and {@code updatedBy} populated
 *       from the {@link org.springframework.data.domain.AuditorAware}
 *       bean registered in the application context.</li>
 * </ul>
 *
 * @author Employee Management Portal Team
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class BaseEntity implements Serializable {

    /**
     * Universally unique identifier (UUID v4) serving as the primary key.
     * Generated automatically by Hibernate using the UUID strategy.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false, length = 36)
    private UUID id;

    /**
     * Timestamp of record creation. Set once by Spring Data JPA Auditing
     * and never updated afterwards.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp of the most recent update. Automatically refreshed by
     * Spring Data JPA Auditing on every {@code merge} operation.
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Username or system identifier of the principal who created the record.
     */
    @CreatedBy
    @Column(name = "created_by", updatable = false, length = 150)
    private String createdBy;

    /**
     * Username or system identifier of the principal who last modified the record.
     */
    @LastModifiedBy
    @Column(name = "updated_by", length = 150)
    private String updatedBy;
}
