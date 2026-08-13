/**
 * @fileoverview EmployeeEmptyState — shown when the employee list is empty.
 *
 * Renders a context-aware message:
 * - When a search or filter is active → "No results found" with a clear action.
 * - When there are truly no employees → "No employees yet" with an add action.
 */

import React from 'react';
import { Box, Button, Typography } from '@mui/material';
import PeopleIcon from '@mui/icons-material/People';
import SearchOffIcon from '@mui/icons-material/SearchOff';

/**
 * @typedef {Object} EmployeeEmptyStateProps
 * @property {boolean}    [hasFilters] - Whether any search / filter is active.
 * @property {() => void} [onClear]    - Callback to clear all filters.
 * @property {() => void} [onAdd]      - Callback to open the create dialog.
 * @property {boolean}    [canCreate]  - Whether to show the Add Employee button.
 */

/**
 * Empty state component for the employee list.
 *
 * @param {EmployeeEmptyStateProps} props
 * @returns {JSX.Element}
 */
export default function EmployeeEmptyState({
  hasFilters = false,
  onClear,
  onAdd,
  canCreate = false,
}) {
  return (
    <Box
      sx={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        py: 10,
        gap: 2,
      }}
      role="status"
      aria-label={hasFilters ? 'No search results' : 'No employees'}
    >
      {hasFilters ? (
        <SearchOffIcon sx={{ fontSize: 52, opacity: 0.2, color: 'text.secondary' }} />
      ) : (
        <PeopleIcon sx={{ fontSize: 52, opacity: 0.2, color: 'text.secondary' }} />
      )}

      <Typography variant="h6" fontWeight={700} color="text.secondary">
        {hasFilters ? 'No employees found' : 'No employees yet'}
      </Typography>

      <Typography variant="body2" color="text.disabled" textAlign="center" maxWidth={380}>
        {hasFilters
          ? "Try adjusting your search or filters to find what you're looking for."
          : 'Start building your team by adding the first employee to the portal.'}
      </Typography>

      <Box sx={{ display: 'flex', gap: 1.5, mt: 1 }}>
        {hasFilters && onClear && (
          <Button variant="outlined" onClick={onClear} aria-label="Clear all filters">
            Clear Filters
          </Button>
        )}
        {canCreate && onAdd && (
          <Button variant="contained" onClick={onAdd} aria-label="Add first employee">
            Add Employee
          </Button>
        )}
      </Box>
    </Box>
  );
}
