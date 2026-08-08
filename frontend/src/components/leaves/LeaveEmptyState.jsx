/**
 * @fileoverview LeaveEmptyState — shown when the leave list is empty.
 */

import React from 'react';
import { Box, Button, Typography } from '@mui/material';
import EventNoteIcon from '@mui/icons-material/EventNote';
import SearchOffIcon from '@mui/icons-material/SearchOff';

/**
 * @typedef {Object} LeaveEmptyStateProps
 * @property {boolean}    [hasFilters]
 * @property {() => void} [onClear]
 * @property {() => void} [onAdd]
 * @property {boolean}    [canCreate]
 */

/**
 * Empty state for the leave list.
 *
 * @param {LeaveEmptyStateProps} props
 * @returns {JSX.Element}
 */
export default function LeaveEmptyState({ hasFilters = false, onClear, onAdd, canCreate = false }) {
  return (
    <Box
      sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center',
            py: 10, gap: 2, color: 'text.disabled' }}
      role="status"
      aria-label={hasFilters ? 'No leave results' : 'No leave requests'}
    >
      {hasFilters
        ? <SearchOffIcon sx={{ fontSize: 64, opacity: 0.3 }} />
        : <EventNoteIcon sx={{ fontSize: 64, opacity: 0.3 }} />}

      <Typography variant="h6" fontWeight={700} color="text.secondary">
        {hasFilters ? 'No leave requests found' : 'No leave requests yet'}
      </Typography>

      <Typography variant="body2" color="text.disabled" textAlign="center" maxWidth={380}>
        {hasFilters
          ? 'Try adjusting your search or filters.'
          : 'No leave requests have been submitted yet.'}
      </Typography>

      <Box sx={{ display: 'flex', gap: 1.5, mt: 1 }}>
        {hasFilters && onClear && (
          <Button variant="outlined" onClick={onClear} aria-label="Clear filters">
            Clear Filters
          </Button>
        )}
        {canCreate && onAdd && (
          <Button variant="contained" onClick={onAdd} aria-label="Request leave">
            Request Leave
          </Button>
        )}
      </Box>
    </Box>
  );
}
