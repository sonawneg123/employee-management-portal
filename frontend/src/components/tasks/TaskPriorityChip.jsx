/**
 * @fileoverview TaskPriorityChip — coloured chip displaying task priority.
 *
 * Phase 6C: URGENT replaces CRITICAL. CRITICAL kept as backward-compat alias.
 */

import React from 'react';
import Chip from '@mui/material/Chip';

const PRIORITY_CONFIG = {
  LOW:      { label: 'Low',    color: 'default',  variant: 'outlined' },
  MEDIUM:   { label: 'Medium', color: 'info',     variant: 'outlined' },
  HIGH:     { label: 'High',   color: 'warning',  variant: 'outlined' },
  URGENT:   { label: 'Urgent', color: 'error',    variant: 'filled'   },
  // Backward-compat alias — pre-6C tasks may still carry CRITICAL
  CRITICAL: { label: 'Critical', color: 'error',  variant: 'outlined' },
};

/**
 * Displays a task's priority as a coloured MUI Chip.
 *
 * @param {{ priority: string, size?: 'small'|'medium' }} props
 * @returns {JSX.Element}
 */
export default function TaskPriorityChip({ priority, size = 'small' }) {
  const config = PRIORITY_CONFIG[priority] ?? { label: priority, color: 'default', variant: 'outlined' };
  return (
    <Chip
      label={config.label}
      color={config.color}
      size={size}
      variant={config.variant}
      sx={{ fontWeight: 500 }}
    />
  );
}
