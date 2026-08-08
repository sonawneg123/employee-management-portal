/**
 * @fileoverview DepartmentFilters — sort field and direction selectors.
 *
 * Departments have no status enum filter, so this component provides
 * sort-field and sort-direction controls only.
 */

import React from 'react';
import { Box, FormControl, InputLabel, MenuItem, Select } from '@mui/material';
import SortIcon from '@mui/icons-material/Sort';
import {
  DEPARTMENT_SORT_OPTIONS,
  DEPARTMENT_DEFAULT_SORT,
  DEPARTMENT_DEFAULT_DIRECTION,
} from '@/constants/departmentConstants';

/**
 * @typedef {Object} DepartmentFiltersProps
 * @property {string}            sort
 * @property {'asc'|'desc'}      direction
 * @property {(v: string) => void} onSortChange
 * @property {(v: string) => void} onDirectionChange
 * @property {boolean}           [disabled]
 */

/**
 * Sort field + direction filter controls for the department list.
 *
 * @param {DepartmentFiltersProps} props
 * @returns {JSX.Element}
 */
export default function DepartmentFilters({
  sort,
  direction,
  onSortChange,
  onDirectionChange,
  disabled = false,
}) {
  return (
    <Box sx={{ display: 'flex', gap: 1.5, flexWrap: 'wrap' }}>
      <FormControl size="small" sx={{ minWidth: 150 }} disabled={disabled}>
        <InputLabel id="dept-sort-label">
          <SortIcon fontSize="small" sx={{ mr: 0.5, verticalAlign: 'middle' }} />
          Sort By
        </InputLabel>
        <Select
          labelId="dept-sort-label"
          value={sort ?? DEPARTMENT_DEFAULT_SORT}
          label="Sort By"
          onChange={(e) => onSortChange(e.target.value)}
          aria-label="Sort departments by"
        >
          {DEPARTMENT_SORT_OPTIONS.map((opt) => (
            <MenuItem key={opt.value} value={opt.value}>{opt.label}</MenuItem>
          ))}
        </Select>
      </FormControl>

      <FormControl size="small" sx={{ minWidth: 100 }} disabled={disabled}>
        <InputLabel id="dept-dir-label">Order</InputLabel>
        <Select
          labelId="dept-dir-label"
          value={direction ?? DEPARTMENT_DEFAULT_DIRECTION}
          label="Order"
          onChange={(e) => onDirectionChange(e.target.value)}
          aria-label="Sort direction"
        >
          <MenuItem value="asc">A → Z</MenuItem>
          <MenuItem value="desc">Z → A</MenuItem>
        </Select>
      </FormControl>
    </Box>
  );
}
