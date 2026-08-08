/**
 * @fileoverview Department table column definitions.
 *
 * Returns the column configuration array consumed by {@link DepartmentTable}.
 * Defined at module scope so the array reference is stable across renders.
 */

import React from 'react';
import { Box, Chip, Typography } from '@mui/material';
import DepartmentAvatar from '@/components/departments/DepartmentAvatar';
import {
  formatDeptCode,
  formatEmployeeCount,
  formatDeptCreatedAt,
  formatHeadName,
} from '@/utils/departmentFormatters';
import { DEPARTMENT_COLUMNS } from '@/constants/departmentConstants';

/**
 * @typedef {Object} DeptTableColumn
 * @property {string}  id         - Unique column key (also used as sort field when sortable).
 * @property {string}  label      - Header text.
 * @property {boolean} [sortable] - Whether the column supports server-side sorting.
 * @property {'left'|'center'|'right'} [align]
 * @property {string}  [width]    - Optional CSS min-width hint.
 * @property {(row: import('@/services/departmentApi').DepartmentResponse) => React.ReactNode} render
 */

/**
 * Returns the full column definition array for the department table.
 *
 * @returns {DeptTableColumn[]}
 */
export function getDepartmentColumns() {
  return [
    {
      id:       DEPARTMENT_COLUMNS.AVATAR,
      label:    '',
      sortable: false,
      width:    '52px',
      render:   (row) => (
        <DepartmentAvatar name={row.name} size={40} />
      ),
    },
    {
      id:       DEPARTMENT_COLUMNS.NAME,
      label:    'Department',
      sortable: true,
      width:    '220px',
      render:   (row) => (
        <Box>
          <Typography variant="body2" fontWeight={600}>
            {row.name}
          </Typography>
          {row.description && (
            <Typography
              variant="caption"
              color="text.secondary"
              sx={{
                display: '-webkit-box',
                WebkitLineClamp: 1,
                WebkitBoxOrient: 'vertical',
                overflow: 'hidden',
              }}
            >
              {row.description}
            </Typography>
          )}
        </Box>
      ),
    },
    {
      id:       DEPARTMENT_COLUMNS.CODE,
      label:    'Code',
      sortable: true,
      width:    '100px',
      render:   (row) => (
        <Chip
          label={formatDeptCode(row.code)}
          size="small"
          variant="outlined"
          sx={{ fontFamily: 'monospace', fontWeight: 700 }}
          aria-label={`Code: ${row.code}`}
        />
      ),
    },
    {
      id:       DEPARTMENT_COLUMNS.EMPLOYEE_COUNT,
      label:    'Employees',
      sortable: false,
      align:    'center',
      width:    '110px',
      render:   (row) => (
        <Typography variant="body2" fontWeight={600} color="primary.main">
          {row.employeeCount ?? '—'}
        </Typography>
      ),
    },
    {
      id:       DEPARTMENT_COLUMNS.HEAD,
      label:    'Head',
      sortable: false,
      width:    '160px',
      render:   (row) => (
        <Typography variant="body2">
          {formatHeadName(row.headName)}
        </Typography>
      ),
    },
    {
      id:       DEPARTMENT_COLUMNS.CREATED_AT,
      label:    'Created',
      sortable: true,
      width:    '130px',
      render:   (row) => (
        <Typography variant="body2">
          {formatDeptCreatedAt(row.createdAt)}
        </Typography>
      ),
    },
  ];
}
