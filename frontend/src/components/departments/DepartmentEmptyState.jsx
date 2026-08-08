/**
 * @fileoverview DepartmentEmptyState — shown when the department list is empty.
 */

import React from 'react';
import { Box, Button, Typography } from '@mui/material';
import ApartmentIcon from '@mui/icons-material/Apartment';
import SearchOffIcon from '@mui/icons-material/SearchOff';

/**
 * @typedef {Object} DepartmentEmptyStateProps
 * @property {boolean}    [hasFilters]
 * @property {() => void} [onClear]
 * @property {() => void} [onAdd]
 * @property {boolean}    [canCreate]
 */

/**
 * Empty state for the department list.
 *
 * @param {DepartmentEmptyStateProps} props
 * @returns {JSX.Element}
 */
export default function DepartmentEmptyState({
  hasFilters = false,
  onClear,
  onAdd,
  canCreate = false,
}) {
  return (
    <Box
      sx={{
        display:        'flex',
        flexDirection:  'column',
        alignItems:     'center',
        justifyContent: 'center',
        py: 10,
        gap: 2,
        color: 'text.disabled',
      }}
      role="status"
      aria-label={hasFilters ? 'No department search results' : 'No departments'}
    >
      {hasFilters ? (
        <SearchOffIcon sx={{ fontSize: 64, opacity: 0.3 }} />
      ) : (
        <ApartmentIcon sx={{ fontSize: 64, opacity: 0.3 }} />
      )}

      <Typography variant="h6" fontWeight={700} color="text.secondary">
        {hasFilters ? 'No departments found' : 'No departments yet'}
      </Typography>

      <Typography variant="body2" color="text.disabled" textAlign="center" maxWidth={380}>
        {hasFilters
          ? "Try adjusting your search to find the department you're looking for."
          : 'Start organising your company by creating the first department.'}
      </Typography>

      <Box sx={{ display: 'flex', gap: 1.5, mt: 1 }}>
        {hasFilters && onClear && (
          <Button variant="outlined" onClick={onClear} aria-label="Clear search">
            Clear Search
          </Button>
        )}
        {canCreate && onAdd && (
          <Button variant="contained" onClick={onAdd} aria-label="Add first department">
            Add Department
          </Button>
        )}
      </Box>
    </Box>
  );
}
