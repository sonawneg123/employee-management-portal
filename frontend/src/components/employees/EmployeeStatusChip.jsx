/**
 * @fileoverview EmployeeStatusChip — soft status badge.
 *
 * Uses the new brand-aligned soft color palette:
 *   ACTIVE    → soft emerald
 *   INACTIVE  → soft amber
 *   ON_LEAVE  → soft indigo
 *   TERMINATED→ soft red
 */

import React from 'react';
import { Box, Typography } from '@mui/material';

/** Soft badge styling map */
const STATUS_STYLES = {
  ACTIVE: {
    label: 'Active',
    color: '#059669',
    bg: 'rgba(16,185,129,0.1)',
    border: 'rgba(16,185,129,0.25)',
    dot: '#10B981',
  },
  DISABLED: {
    label: 'Disabled',
    color: '#64748B',
    bg: 'rgba(100,116,139,0.1)',
    border: 'rgba(100,116,139,0.25)',
    dot: '#94A3B8',
  },
  INACTIVE: {
    label: 'Inactive',
    color: '#D97706',
    bg: 'rgba(245,158,11,0.1)',
    border: 'rgba(245,158,11,0.25)',
    dot: '#F59E0B',
  },
  ON_LEAVE: {
    label: 'On Leave',
    color: '#2D3A6B',
    bg: 'rgba(26,35,66,0.1)',
    border: 'rgba(26,35,66,0.2)',
    dot: '#4F6AB5',
  },
  TERMINATED: {
    label: 'Terminated',
    color: '#DC2626',
    bg: 'rgba(239,68,68,0.1)',
    border: 'rgba(239,68,68,0.25)',
    dot: '#EF4444',
  },
};

/**
 * @param {{ status: string, size?: 'small' | 'medium' }} props
 */
export default function EmployeeStatusChip({ status, size = 'medium' }) {
  const s = STATUS_STYLES[status] ?? {
    label: status ?? '—',
    color: '#64748B',
    bg: 'rgba(100,116,139,0.08)',
    border: 'rgba(100,116,139,0.2)',
    dot: '#94A3B8',
  };

  const isSmall = size === 'small';

  return (
    <Box
      component="span"
      aria-label={`Status: ${s.label}`}
      sx={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: 0.6,
        px: isSmall ? 0.85 : 1.1,
        py: isSmall ? 0.2 : 0.35,
        borderRadius: '6px',
        bgcolor: s.bg,
        border: `1px solid ${s.border}`,
        userSelect: 'none',
      }}
    >
      <Box
        aria-hidden="true"
        sx={{
          width: isSmall ? 5 : 6,
          height: isSmall ? 5 : 6,
          borderRadius: '50%',
          bgcolor: s.dot,
          flexShrink: 0,
        }}
      />
      <Typography
        component="span"
        sx={{
          fontSize: isSmall ? '0.675rem' : '0.75rem',
          fontWeight: 700,
          color: s.color,
          lineHeight: 1,
          letterSpacing: '0.01em',
        }}
      >
        {s.label}
      </Typography>
    </Box>
  );
}
