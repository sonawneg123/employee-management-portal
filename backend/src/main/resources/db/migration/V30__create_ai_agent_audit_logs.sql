-- Phase 7E: AI Agent Audit Log table
-- Records every agentic AI execution for observability and security auditing.
-- Sensitive data (passwords, tokens, salaries) is NEVER stored here.

CREATE TABLE IF NOT EXISTS ai_agent_audit_logs (
    id                          CHAR(36)        NOT NULL DEFAULT (UUID())          COMMENT 'Primary key',
    user_id                     CHAR(36)        NOT NULL                            COMMENT 'Authenticated user UUID',
    username                    VARCHAR(255)    NOT NULL                            COMMENT 'Authenticated user email',
    user_role                   VARCHAR(50)     NOT NULL                            COMMENT 'Primary role of the user',
    user_request                VARCHAR(2000)   NOT NULL                            COMMENT 'Original user message (truncated)',
    tools_invoked               VARCHAR(1000)   NULL                                COMMENT 'Comma-separated tool names invoked',
    tool_args_summary           VARCHAR(2000)   NULL                                COMMENT 'Sanitised tool argument summary',
    tool_results_summary        VARCHAR(4000)   NULL                                COMMENT 'Summary of tool results',
    response_type               VARCHAR(50)     NULL                                COMMENT 'INFORMATION|RECOMMENDATION|ACTION_PROPOSAL|ACTION_COMPLETED|ERROR',
    proposed_action_type        VARCHAR(100)    NULL                                COMMENT 'Action type proposed',
    proposed_action_resource_id VARCHAR(100)    NULL                                COMMENT 'Resource ID targeted by action',
    action_confirmed            TINYINT(1)      NULL                                COMMENT 'Whether user confirmed the action',
    action_success              TINYINT(1)      NULL                                COMMENT 'Whether the action succeeded',
    failure_reason              VARCHAR(500)    NULL                                COMMENT 'Failure reason if applicable',
    tool_call_count             INT             NOT NULL DEFAULT 0                  COMMENT 'Number of tool calls in this execution',
    execution_ms                BIGINT          NOT NULL DEFAULT 0                  COMMENT 'Execution duration in milliseconds',
    created_at                  DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Audit record timestamp',

    PRIMARY KEY (id),
    INDEX idx_aaal_user_id    (user_id),
    INDEX idx_aaal_created_at (created_at),
    INDEX idx_aaal_username   (username(100))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='AI Agent Copilot audit log — Phase 7E';
