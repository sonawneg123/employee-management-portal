-- ─────────────────────────────────────────────────────────────
--  Flyway Migration V13 — Password Reset / OTP Table
--  Employee Management Portal
-- ─────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id              CHAR(36)        NOT NULL,
    email           VARCHAR(150)    NOT NULL,
    otp_hash        VARCHAR(255)    NOT NULL   COMMENT 'BCrypt hash of the 6-digit OTP',
    expires_at      DATETIME(6)     NOT NULL   COMMENT 'OTP expiry timestamp (10 minutes after creation)',
    attempt_count   INT             NOT NULL   DEFAULT 0   COMMENT 'Number of failed verification attempts',
    verified        TINYINT(1)      NOT NULL   DEFAULT 0   COMMENT '1 = OTP has been verified; reset token is valid',
    used            TINYINT(1)      NOT NULL   DEFAULT 0   COMMENT '1 = reset has been consumed; record is spent',
    created_at      DATETIME(6)     NOT NULL,
    updated_at      DATETIME(6)     NOT NULL,
    created_by      VARCHAR(150)        NULL,
    updated_by      VARCHAR(150)        NULL,
    PRIMARY KEY (id),
    INDEX idx_prt_email (email),
    INDEX idx_prt_email_active (email, used, verified)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
