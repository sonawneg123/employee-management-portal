/**
 * @fileoverview EmployeeStatusChip — MUI Chip showing an employee's status.
 *
 * Colour and label are derived from the {@link EMPLOYEE_STATUS_MAP} lookup
 * so adding a new status requires only updating the constants file.
 */

import React from 'react';
import { Chip } from '@mui/material';
import { EMPLOYEE_STATUS_MAP } from '@/constants/employeeConstants';

/**
 * @typedef {Object} EmployeeStatusChipProps
 * @property {string}           status   - Raw status enum value from the API.
 * @property {'small'|'medium'} [size]   - MUI Chip size prop.
 */

/**
 * Coloured status chip for an employee.
 *
 * @param {EmployeeStatusChipProps} props
 * @returns {JSX.Element}
 */
export default function EmployeeStatusChip({ status, size = 'medium' }) {
  const meta  = EMPLOYEE_STATUS_MAP[status];
  const label = meta?.label ?? status ?? '—';
  const color = meta?.color ?? 'default';

  return (
    <Chip
      label={label}
      color={color}
      size={size}
      aria-label={`Status: ${label}`}
      sx={{ fontWeight: 600, letterSpacing: 0.2 }}
    />
  );
}
