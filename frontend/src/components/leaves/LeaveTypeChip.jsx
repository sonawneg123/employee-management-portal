/**
 * @fileoverview LeaveTypeChip — MUI Chip showing the leave type with emoji icon.
 */

import React from 'react';
import { Chip } from '@mui/material';
import { LEAVE_TYPE_MAP } from '@/constants/leaveConstants';

/**
 * @typedef {Object} LeaveTypeChipProps
 * @property {string}           type
 * @property {'small'|'medium'} [size]
 */

/**
 * Coloured leave type chip with emoji icon prefix.
 *
 * @param {LeaveTypeChipProps} props
 * @returns {JSX.Element}
 */
export default function LeaveTypeChip({ type, size = 'medium' }) {
  const meta  = LEAVE_TYPE_MAP[type];
  const label = meta ? `${meta.icon} ${meta.label}` : (type ?? '—');
  const color = meta?.color ?? 'default';

  return (
    <Chip
      label={label}
      color={color}
      size={size}
      variant="outlined"
      aria-label={`Leave type: ${meta?.label ?? type}`}
      sx={{ fontWeight: 600 }}
    />
  );
}
