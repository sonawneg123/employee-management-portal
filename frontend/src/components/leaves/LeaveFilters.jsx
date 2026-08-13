/**
 * @fileoverview LeaveFilters — type and status filter dropdowns for the leave list.
 */

import React from 'react';
import { Box, FormControl, InputLabel, MenuItem, Select } from '@mui/material';
import { LEAVE_TYPE_OPTIONS, LEAVE_STATUS_OPTIONS } from '@/constants/leaveConstants';

/**
 * @typedef {Object} LeaveFiltersProps
 * @property {string}              status
 * @property {string}              type
 * @property {(v: string) => void} onStatusChange
 * @property {(v: string) => void} onTypeChange
 * @property {boolean}             [disabled]
 */

/**
 * Leave type and status filter controls.
 *
 * @param {LeaveFiltersProps} props
 * @returns {JSX.Element}
 */
export default function LeaveFilters({
  status,
  type,
  onStatusChange,
  onTypeChange,
  disabled = false,
}) {
  return (
    <Box sx={{ display: 'flex', gap: 1.5, flexWrap: 'wrap' }}>
      <FormControl size="small" sx={{ minWidth: 150 }} disabled={disabled}>
        <InputLabel id="leave-type-label">Type</InputLabel>
        <Select
          labelId="leave-type-label"
          value={type}
          label="Type"
          onChange={(e) => onTypeChange(e.target.value)}
          aria-label="Filter by leave type"
        >
          <MenuItem value="">All Types</MenuItem>
          {LEAVE_TYPE_OPTIONS.map((opt) => (
            <MenuItem key={opt.value} value={opt.value}>
              {opt.icon} {opt.label}
            </MenuItem>
          ))}
        </Select>
      </FormControl>

      <FormControl size="small" sx={{ minWidth: 140 }} disabled={disabled}>
        <InputLabel id="leave-status-label">Status</InputLabel>
        <Select
          labelId="leave-status-label"
          value={status}
          label="Status"
          onChange={(e) => onStatusChange(e.target.value)}
          aria-label="Filter by status"
        >
          <MenuItem value="">All Statuses</MenuItem>
          {LEAVE_STATUS_OPTIONS.map((opt) => (
            <MenuItem key={opt.value} value={opt.value}>
              {opt.label}
            </MenuItem>
          ))}
        </Select>
      </FormControl>
    </Box>
  );
}
