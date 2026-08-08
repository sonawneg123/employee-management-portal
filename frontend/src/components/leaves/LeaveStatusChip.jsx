/**
 * @fileoverview LeaveStatusChip — MUI Chip showing a leave request's status.
 */

import React from 'react';
import { Chip } from '@mui/material';
import { LEAVE_STATUS_MAP } from '@/constants/leaveConstants';

/**
 * @typedef {Object} LeaveStatusChipProps
 * @property {string}           status
 * @property {'small'|'medium'} [size]
 */

/**
 * Coloured status chip for a leave request.
 *
 * @param {LeaveStatusChipProps} props
 * @returns {JSX.Element}
 */
export default function LeaveStatusChip({ status, size = 'medium' }) {
  const meta  = LEAVE_STATUS_MAP[status];
  const label = meta?.label ?? status ?? '—';
  const color = meta?.color ?? 'default';

  return (
    <Chip
      label={label}
      color={color}
      size={size}
      aria-label={`Status: ${label}`}
      sx={{ fontWeight: 600 }}
    />
  );
}
