-- V28: Add DISABLED status to the employees.status ENUM column.
--
-- Root cause: V1 defined status as ENUM('ACTIVE','INACTIVE','ON_LEAVE','TERMINATED').
-- The Java EmployeeStatus enum gained the DISABLED value (used by AdminServiceImpl
-- when an admin disables a user account), but the MySQL column was never updated,
-- causing a "Data truncated for column 'status'" error on every disable attempt.
--
-- Safe for existing data: MODIFY COLUMN keeps the existing allowed values
-- and adds DISABLED. All current rows remain valid (ACTIVE / INACTIVE / ON_LEAVE /
-- TERMINATED) and are not touched.

ALTER TABLE employees
    MODIFY COLUMN status
        ENUM('ACTIVE','INACTIVE','ON_LEAVE','TERMINATED','DISABLED')
        NOT NULL DEFAULT 'ACTIVE';
