/**
 * @fileoverview LeaveStatusChip — soft badge for a leave request's status.
 */

import React from 'react';
import { Box } from '@mui/material';
import { LEAVE_STATUS_MAP } from '@/constants/leaveConstants';

/** Soft palette per status */
const SOFT = {
  PENDING: { bg: '#FEF3C7', color: '#92400E', dot: '#F59E0B' },
  APPROVED: { bg: '#D1FAE5', color: '#065F46', dot: '#10B981' },
  REJECTED: { bg: '#FEE2E2', color: '#991B1B', dot: '#EF4444' },
  CANCELLED: { bg: '#F1F5F9', color: '#475569', dot: '#94A3B8' },
};

/**
 * @typedef {Object} LeaveStatusChipProps
 * @property {string}           status
 * @property {'small'|'medium'} [size]
 */

/**
 * Soft-badge status indicator for a leave request.
 *
 * @param {LeaveStatusChipProps} props
 * @returns {JSX.Element}
 */
export default function LeaveStatusChip({ status, size = 'medium' }) {
  const meta = LEAVE_STATUS_MAP[status];
  const label = meta?.label ?? status ?? '—';
  const palette = SOFT[status] ?? { bg: '#F1F5F9', color: '#475569', dot: '#94A3B8' };

  const isSmall = size === 'small';

  return (
    <Box
      component="span"
      aria-label={`Status: ${label}`}
      sx={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: '5px',
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
      <Box
        component="span"
        sx={{
          width: isSmall ? 5 : 6,
          height: isSmall ? 5 : 6,
          borderRadius: '50%',
          bgcolor: palette.dot,
          flexShrink: 0,
        }}
      />
      {label}
    </Box>
  );
}
