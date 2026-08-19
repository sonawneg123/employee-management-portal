/**
 * @fileoverview DashboardHeader — page header with title, live indicator, and refresh.
 */

import React from 'react';
import { Box, IconButton, Tooltip, Typography } from '@mui/material';
import RefreshRoundedIcon from '@mui/icons-material/RefreshRounded';
import FiberManualRecordRoundedIcon from '@mui/icons-material/FiberManualRecord';
import { formatRelative } from '@/utils/dateUtils';

/**
 * @typedef {Object} DashboardHeaderProps
 * @property {string}    [lastUpdated]
 * @property {boolean}   [isFetching]
 * @property {() => void} onRefresh
 */

/**
 * Dashboard page header.
 *
 * @param {DashboardHeaderProps} props
 * @returns {JSX.Element}
 */
export default function DashboardHeader({ lastUpdated, isFetching, onRefresh }) {
  return (
    <Box
      sx={{
        display: 'flex',
        alignItems: 'flex-start',
        justifyContent: 'space-between',
        mb: 3,
        flexWrap: 'wrap',
        gap: 1,
      }}
    >
      <Box>
        <Typography
          variant="h2"
          fontWeight={800}
          sx={{
            letterSpacing: '-0.03em',
            mb: 0.25,
            color: 'text.primary',
            fontSize: { xs: '1.5rem', sm: '1.75rem' },
          }}
        >
          Dashboard
        </Typography>
        {lastUpdated && (
          <Typography variant="caption" color="text.secondary">
            Last updated {formatRelative(lastUpdated)}
          </Typography>
        )}
      </Box>

      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
        {/* Live indicator */}
        <Box
          sx={{
            display: 'flex',
            alignItems: 'center',
            gap: 0.6,
            border: '1.5px solid',
            borderColor: isFetching ? 'warning.main' : 'success.main',
            borderRadius: '8px',
            px: 1.25,
            py: 0.5,
            bgcolor: isFetching ? 'rgba(245,158,11,0.08)' : 'rgba(16,185,129,0.08)',
          }}
          aria-live="polite"
          aria-label={isFetching ? 'Dashboard is updating' : 'Dashboard data is live'}
        >
          <FiberManualRecordRoundedIcon
            sx={{
              fontSize: 8,
              color: isFetching ? 'warning.main' : 'success.main',
              animation: isFetching ? 'pulse 1.2s ease-in-out infinite' : 'none',
              '@keyframes pulse': {
                '0%, 100%': { opacity: 1 },
                '50%': { opacity: 0.3 },
              },
            }}
          />
          <Typography
            variant="caption"
            fontWeight={600}
            color={isFetching ? 'warning.main' : 'success.main'}
            sx={{ fontSize: '0.7rem' }}
          >
            {isFetching ? 'Updating…' : 'Live'}
          </Typography>
        </Box>

        {/* Refresh button */}
        <Tooltip title="Refresh dashboard data">
          <IconButton
            onClick={onRefresh}
            disabled={isFetching}
            size="small"
            aria-label="Refresh dashboard data"
            sx={{
              bgcolor: 'action.hover',
              borderRadius: '8px',
              width: 36,
              height: 36,
              animation: isFetching ? 'spin 1s linear infinite' : 'none',
              '@keyframes spin': {
                '0%': { transform: 'rotate(0deg)' },
                '100%': { transform: 'rotate(360deg)' },
              },
              '&:hover': { bgcolor: 'rgba(79,70,229,0.08)', color: 'primary.main' },
            }}
          >
            <RefreshRoundedIcon sx={{ fontSize: 18 }} />
          </IconButton>
        </Tooltip>
      </Box>
    </Box>
  );
}
