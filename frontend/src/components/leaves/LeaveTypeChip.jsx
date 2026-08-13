/**
 * @fileoverview LeaveTypeChip — soft badge showing the leave type with emoji icon.
 */

import React from 'react';
import { Box } from '@mui/material';
import { LEAVE_TYPE_MAP } from '@/constants/leaveConstants';

/** Soft background per type */
const TYPE_PALETTE = {
  ANNUAL: { bg: '#EDE9FE', color: '#5B21B6' },
  SICK: { bg: '#FEE2E2', color: '#991B1B' },
  MATERNITY: { bg: '#FCE7F3', color: '#9D174D' },
  PATERNITY: { bg: '#DBEAFE', color: '#1E40AF' },
  UNPAID: { bg: '#FEF3C7', color: '#92400E' },
  EMERGENCY: { bg: '#FEE2E2', color: '#7F1D1D' },
  STUDY: { bg: '#D1FAE5', color: '#065F46' },
  OTHER: { bg: '#F1F5F9', color: '#475569' },
};

/**
 * @typedef {Object} LeaveTypeChipProps
 * @property {string}           type
 * @property {'small'|'medium'} [size]
 */

/**
 * Soft-badge leave type chip with emoji icon.
 *
 * @param {LeaveTypeChipProps} props
 * @returns {JSX.Element}
 */
export default function LeaveTypeChip({ type, size = 'medium' }) {
  const meta = LEAVE_TYPE_MAP[type];
  const label = meta?.label ?? type ?? '—';
  const palette = TYPE_PALETTE[type] ?? { bg: '#F1F5F9', color: '#475569' };
  const isSmall = size === 'small';

  return (
    <Box
      component="span"
      aria-label={`Leave type: ${label}`}
      sx={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: '4px',
        px: isSmall ? '7px' : '10px',
        py: isSmall ? '2px' : '4px',
        borderRadius: '20px',
        bgcolor: palette.bg,
        color: palette.color,
        fontSize: isSmall ? '0.7rem' : '0.75rem',
        fontWeight: 600,
        letterSpacing: '0.02em',
        lineHeight: 1.4,
        whiteSpace: 'nowrap',
        userSelect: 'none',
      }}
    >
      {meta?.icon && <span>{meta.icon}</span>}
      {label}
    </Box>
  );
}
