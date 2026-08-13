/**
 * @fileoverview EmployeeToolbar — top action bar for the employee list.
 *
 * Contains the search field, filter dropdowns, sort selector, and right-side
 * action buttons (Add, Refresh, Export CSV). Layout is responsive:
 * - Desktop: single row, space-between.
 * - Mobile: stacked rows.
 */

import React from 'react';
import {
  Box,
  Button,
  Divider,
  FormControl,
  IconButton,
  InputLabel,
  MenuItem,
  Select,
  Tooltip,
  Typography,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import RefreshIcon from '@mui/icons-material/Refresh';
import DownloadIcon from '@mui/icons-material/Download';
import SortIcon from '@mui/icons-material/Sort';
import EmployeeSearch from './EmployeeSearch';
import EmployeeFilters from './EmployeeFilters';
import {
  EMPLOYEE_SORT_OPTIONS,
  EMPLOYEE_DEFAULT_SORT,
  EMPLOYEE_DEFAULT_DIRECTION,
} from '@/constants/employeeConstants';

/**
 * @typedef {Object} EmployeeToolbarProps
 * @property {string}             search         - Controlled search value.
 * @property {string}             departmentId   - Controlled department filter.
 * @property {string}             status         - Controlled status filter.
 * @property {string}             sort           - Current sort field.
 * @property {'asc'|'desc'}       direction      - Current sort direction.
 * @property {number}             totalElements  - Total record count for display.
 * @property {boolean}            isFetching     - Disables controls while fetching.
 * @property {boolean}            canCreate      - Shows the Add button when true.
 * @property {(v: string) => void} onSearchChange
 * @property {(v: string) => void} onDepartmentChange
 * @property {(v: string) => void} onStatusChange
 * @property {(v: string) => void} onSortChange
 * @property {(v: string) => void} onDirectionChange
 * @property {() => void}         onAdd
 * @property {() => void}         onRefresh
 * @property {() => void}         onExport
 * @property {() => void}         onClearFilters
 */

/**
 * Toolbar above the employee table.
 *
 * @param {EmployeeToolbarProps} props
 * @returns {JSX.Element}
 */
export default function EmployeeToolbar({
  search,
  departmentId,
  status,
  sort,
  direction,
  totalElements,
  isFetching,
  canCreate,
  onSearchChange,
  onDepartmentChange,
  onStatusChange,
  onSortChange,
  onDirectionChange,
  onAdd,
  onRefresh,
  onExport,
  onClearFilters,
}) {
  const hasActiveFilters = Boolean(search || departmentId || status);

  return (
    <Box sx={{ p: 2 }}>
      {/* Row 1: search + filters */}
      <Box
        sx={{
          display: 'flex',
          flexWrap: 'wrap',
          gap: 1.5,
          alignItems: 'center',
          mb: 1.5,
        }}
      >
        <EmployeeSearch value={search} onSearch={onSearchChange} disabled={isFetching} />
        <EmployeeFilters
          departmentId={departmentId}
          status={status}
          onDepartmentChange={onDepartmentChange}
          onStatusChange={onStatusChange}
          disabled={isFetching}
        />

        {hasActiveFilters && (
          <Button
            size="small"
            variant="text"
            color="secondary"
            onClick={onClearFilters}
            aria-label="Clear all filters"
          >
            Clear
          </Button>
        )}

        {/* Sort selector */}
        <Box sx={{ display: 'flex', gap: 1, ml: { sm: 'auto' }, flexShrink: 0 }}>
          <FormControl size="small" sx={{ minWidth: 140 }} disabled={isFetching}>
            <InputLabel id="sort-label">
              <SortIcon fontSize="small" sx={{ mr: 0.5, verticalAlign: 'middle' }} />
              Sort
            </InputLabel>
            <Select
              labelId="sort-label"
              value={sort ?? EMPLOYEE_DEFAULT_SORT}
              label="Sort"
              onChange={(e) => onSortChange(e.target.value)}
              aria-label="Sort by field"
            >
              {EMPLOYEE_SORT_OPTIONS.map((opt) => (
                <MenuItem key={opt.value} value={opt.value}>
                  {opt.label}
                </MenuItem>
              ))}
            </Select>
          </FormControl>

          <FormControl size="small" sx={{ minWidth: 100 }} disabled={isFetching}>
            <InputLabel id="dir-label">Order</InputLabel>
            <Select
              labelId="dir-label"
              value={direction ?? EMPLOYEE_DEFAULT_DIRECTION}
              label="Order"
              onChange={(e) => onDirectionChange(e.target.value)}
              aria-label="Sort direction"
            >
              <MenuItem value="asc">A → Z</MenuItem>
              <MenuItem value="desc">Z → A</MenuItem>
            </Select>
          </FormControl>
        </Box>
      </Box>

      <Divider />

      {/* Row 2: count + action buttons */}
      <Box
        sx={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          mt: 1.5,
          flexWrap: 'wrap',
          gap: 1,
        }}
      >
        <Typography variant="body2" color="text.secondary">
          {totalElements != null
            ? `${totalElements} employee${totalElements !== 1 ? 's' : ''}`
            : ''}
        </Typography>

        <Box sx={{ display: 'flex', gap: 1 }}>
          <Tooltip title="Refresh">
            <IconButton
              size="small"
              onClick={onRefresh}
              disabled={isFetching}
              aria-label="Refresh employee list"
              sx={{
                animation: isFetching ? 'spin 1s linear infinite' : 'none',
                '@keyframes spin': {
                  '0%': { transform: 'rotate(0deg)' },
                  '100%': { transform: 'rotate(360deg)' },
                },
              }}
            >
              <RefreshIcon fontSize="small" />
            </IconButton>
          </Tooltip>

          <Tooltip title="Export CSV">
            <IconButton
              size="small"
              onClick={onExport}
              disabled={isFetching || !totalElements}
              aria-label="Export employees to CSV"
            >
              <DownloadIcon fontSize="small" />
            </IconButton>
          </Tooltip>

          {canCreate && (
            <Button
              variant="contained"
              size="small"
              startIcon={<AddIcon />}
              onClick={onAdd}
              aria-label="Add new employee"
            >
              Add Employee
            </Button>
          )}
        </Box>
      </Box>
    </Box>
  );
}
