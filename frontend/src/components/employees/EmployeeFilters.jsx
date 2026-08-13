/**
 * @fileoverview EmployeeFilters — department and status filter dropdowns.
 *
 * Renders a department select (populated from the departments API) and a
 * status select (static options). Both are controlled components that
 * notify the parent on change so the parent can update the query params.
 */

import React from 'react';
import { Box, FormControl, InputLabel, MenuItem, Select, Skeleton } from '@mui/material';
import { EMPLOYEE_STATUS_OPTIONS } from '@/constants/employeeConstants';
import { useDepartments } from '@/hooks/useDepartments';

/**
 * @typedef {Object} EmployeeFiltersProps
 * @property {string}              departmentId      - Selected department UUID or ''.
 * @property {string}              status            - Selected status value or ''.
 * @property {(id: string) => void}  onDepartmentChange - Called when department selection changes.
 * @property {(status: string) => void} onStatusChange - Called when status selection changes.
 * @property {boolean}             [disabled]
 */

/**
 * Department and status filter controls.
 *
 * @param {EmployeeFiltersProps} props
 * @returns {JSX.Element}
 */
export default function EmployeeFilters({
  departmentId,
  status,
  onDepartmentChange,
  onStatusChange,
  disabled = false,
}) {
  const { data: departments, isLoading: deptsLoading } = useDepartments();

  return (
    <Box sx={{ display: 'flex', gap: 1.5, flexWrap: 'wrap' }}>
      {/* Department filter */}
      <FormControl size="small" sx={{ minWidth: 180 }} disabled={disabled}>
        <InputLabel id="dept-filter-label">Department</InputLabel>
        <Select
          labelId="dept-filter-label"
          value={departmentId}
          label="Department"
          onChange={(e) => onDepartmentChange(e.target.value)}
          aria-label="Filter by department"
        >
          <MenuItem value="">All Departments</MenuItem>
          {deptsLoading ? (
            <MenuItem disabled>
              <Skeleton variant="text" width={120} />
            </MenuItem>
          ) : (
            departments?.map((d) => (
              <MenuItem key={d.id} value={d.id}>
                {d.name}
              </MenuItem>
            ))
          )}
        </Select>
      </FormControl>

      {/* Status filter */}
      <FormControl size="small" sx={{ minWidth: 150 }} disabled={disabled}>
        <InputLabel id="status-filter-label">Status</InputLabel>
        <Select
          labelId="status-filter-label"
          value={status}
          label="Status"
          onChange={(e) => onStatusChange(e.target.value)}
          aria-label="Filter by status"
        >
          <MenuItem value="">All Statuses</MenuItem>
          {EMPLOYEE_STATUS_OPTIONS.map((opt) => (
            <MenuItem key={opt.value} value={opt.value}>
              {opt.label}
            </MenuItem>
          ))}
        </Select>
      </FormControl>
    </Box>
  );
}
