/**
 * @fileoverview TaskStatusChip — coloured chip displaying a task status.
 */

import React from 'react';
import Chip from '@mui/material/Chip';

const STATUS_CONFIG = {
  DRAFT:             { label: 'Draft',              color: 'default' },
  ASSIGNED:          { label: 'Assigned',           color: 'info' },
  IN_PROGRESS:       { label: 'In Progress',        color: 'primary' },
  SUBMITTED:         { label: 'Submitted',          color: 'warning' },
  COMPLETED:         { label: 'Completed',          color: 'success' },
  CHANGES_REQUESTED: { label: 'Changes Requested',  color: 'error' },
  REJECTED:          { label: 'Rejected',           color: 'error' },
};

/**
 * Displays a task's current status as a coloured MUI Chip.
 *
 * @param {{ status: string, size?: 'small'|'medium', overdue?: boolean }} props
 * @returns {JSX.Element}
 */
export default function TaskStatusChip({ status, size = 'small', overdue = false }) {
  if (overdue && status !== 'COMPLETED') {
    return (
      <Chip
        label="Overdue"
        color="error"
        size={size}
        variant="outlined"
        sx={{ fontWeight: 600 }}
      />
    );
  }

  const config = STATUS_CONFIG[status] ?? { label: status, color: 'default' };
  return (
    <Chip
      label={config.label}
      color={config.color}
      size={size}
      sx={{ fontWeight: 500 }}
    />
  );
}
