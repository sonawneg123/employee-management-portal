/**
 * @fileoverview Employee table column definitions.
 *
 * Returns the column configuration array consumed by {@link EmployeeTable}.
 * Each column object defines the header label, sort key, alignment, and
 * an optional render function. Keeping column definitions outside the
 * component prevents re-creation on every render.
 */

import React from 'react';
import { Box, Typography } from '@mui/material';
import EmployeeAvatar       from '@/components/employees/EmployeeAvatar';
import EmployeeStatusChip   from '@/components/employees/EmployeeStatusChip';
import EmployeeDepartmentChip from '@/components/employees/EmployeeDepartmentChip';
import { formatFullName, formatSalary, formatJoinDate } from '@/utils/employeeFormatters';
import { EMPLOYEE_COLUMNS }   from '@/constants/employeeConstants';

/**
 * @typedef {Object} TableColumn
 * @property {string}            id          - Unique column key (matches sort field when sortable).
 * @property {string}            label       - Header text.
 * @property {boolean}           [sortable]  - Whether the column supports server-side sorting.
 * @property {'left'|'center'|'right'} [align] - Cell alignment.
 * @property {string}            [width]     - Optional CSS min-width hint.
 * @property {(row: import('@/services/employeeApi').EmployeeResponse) => React.ReactNode} render
 *   - Function that receives the row data and returns the cell content.
 */

/**
 * Returns the full column definition array for the employee table.
 *
 * Called once at module level so the array reference is stable.
 *
 * @returns {TableColumn[]}
 */
export function getEmployeeColumns() {
  return [
    {
      id:       EMPLOYEE_COLUMNS.EMPLOYEE_CODE,
      label:    'Code',
      sortable: false,
      width:    '100px',
      render:   (row) => (
        <Typography variant="body2" fontFamily="monospace" fontWeight={600}>
          {row.employeeCode}
        </Typography>
      ),
    },
    {
      id:       EMPLOYEE_COLUMNS.FULL_NAME,
      label:    'Employee',
      sortable: true,
      width:    '220px',
      render:   (row) => (
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
          <EmployeeAvatar
            firstName={row.firstName}
            lastName={row.lastName}
            profilePhotoUrl={row.profilePhotoUrl}
            size={36}
          />
          <Box>
            <Typography variant="body2" fontWeight={600} noWrap>
              {formatFullName(row.firstName, row.lastName)}
            </Typography>
            <Typography variant="caption" color="text.secondary" noWrap>
              {row.email ?? '—'}
            </Typography>
          </Box>
        </Box>
      ),
    },
    {
      id:       EMPLOYEE_COLUMNS.JOB_TITLE,
      label:    'Job Title',
      sortable: true,
      width:    '180px',
      render:   (row) => (
        <Typography variant="body2" noWrap>
          {row.jobTitle ?? '—'}
        </Typography>
      ),
    },
    {
      id:       EMPLOYEE_COLUMNS.DEPARTMENT,
      label:    'Department',
      sortable: false,
      width:    '160px',
      render:   (row) => (
        <EmployeeDepartmentChip
          departmentName={row.departmentName}
          size="small"
        />
      ),
    },
    {
      id:       EMPLOYEE_COLUMNS.STATUS,
      label:    'Status',
      sortable: false,
      align:    'center',
      width:    '120px',
      render:   (row) => <EmployeeStatusChip status={row.status} size="small" />,
    },
    {
      id:       EMPLOYEE_COLUMNS.DATE_OF_JOINING,
      label:    'Joined',
      sortable: true,
      width:    '130px',
      render:   (row) => (
        <Typography variant="body2">
          {formatJoinDate(row.dateOfJoining)}
        </Typography>
      ),
    },
    {
      id:       EMPLOYEE_COLUMNS.SALARY,
      label:    'Salary',
      sortable: true,
      align:    'right',
      width:    '130px',
      render:   (row) => (
        <Typography variant="body2" fontWeight={500}>
          {formatSalary(row.salary)}
        </Typography>
      ),
    },
  ];
}
