package com.company.employeemanagement.ai.agent.repository;

import com.company.employeemanagement.ai.agent.entity.AiAgentAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Spring Data JPA repository for {@link AiAgentAuditLog}.
 *
 * @author Employee Management Portal Team
 */
@Repository
public interface AiAgentAuditLogRepository extends JpaRepository<AiAgentAuditLog, UUID> {
}
