/**
 * @fileoverview EmployeeDepartmentChip — outlined chip showing the department name.
 */

import React from 'react';
import { Chip } from '@mui/material';
import ApartmentIcon from '@mui/icons-material/Apartment';

/**
 * @typedef {Object} EmployeeDepartmentChipProps
 * @property {string | null | undefined}  departmentName
 * @property {'small'|'medium'}           [size]
 */

/**
 * Outlined chip displaying an employee's department with a building icon.
 *
 * @param {EmployeeDepartmentChipProps} props
 * @returns {JSX.Element}
 */
export default function EmployeeDepartmentChip({ departmentName, size = 'medium' }) {
  return (
    <Chip
      icon={<ApartmentIcon />}
      label={departmentName ?? '—'}
      size={size}
      variant="outlined"
      aria-label={`Department: ${departmentName ?? 'Unknown'}`}
      sx={{ maxWidth: 160 }}
    />
  );
}
