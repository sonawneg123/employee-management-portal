/**
 * @fileoverview DashboardHeader — page header with title, last-updated time, and refresh button.
 */

import React from 'react';
import { Box, Chip, IconButton, Tooltip, Typography } from '@mui/material';
import RefreshIcon    from '@mui/icons-material/Refresh';
import CircleIcon     from '@mui/icons-material/Circle';
import { formatRelative } from '@/utils/dateUtils';

/**
 * @typedef {Object} DashboardHeaderProps
 * @property {string}    [lastUpdated]   - ISO-8601 timestamp of the most recent data fetch.
 * @property {boolean}   [isFetching]    - Whether any dashboard query is currently refetching.
 * @property {() => void} onRefresh      - Callback to trigger a manual data refresh.
 */

/**
 * Dashboard page header with live-indicator, last-updated time, and refresh button.
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
        <Typography variant="h4" fontWeight={700} gutterBottom>
          Dashboard
        </Typography>
        {lastUpdated && (
          <Typography variant="caption" color="text.secondary">
            Last updated {formatRelative(lastUpdated)}
          </Typography>
        )}
      </Box>

      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
        {/* Live indicator */}
        <Chip
          icon={
            <CircleIcon
              sx={{
                fontSize: '10px !important',
                color: isFetching ? 'warning.main' : 'success.main',
                animation: isFetching ? 'pulse 1.5s ease-in-out infinite' : 'none',
                '@keyframes pulse': {
                  '0%, 100%': { opacity: 1 },
                  '50%':      { opacity: 0.3 },
                },
              }}
            />
          }
          label={isFetching ? 'Updating…' : 'Live'}
          size="small"
          variant="outlined"
          sx={{ borderRadius: 2 }}
          aria-live="polite"
          aria-label={isFetching ? 'Dashboard is updating' : 'Dashboard data is live'}
        />

        {/* Refresh button */}
        <Tooltip title="Refresh all data">
          <IconButton
            onClick={onRefresh}
            disabled={isFetching}
            aria-label="Refresh dashboard data"
            sx={{
              animation: isFetching ? 'spin 1s linear infinite' : 'none',
              '@keyframes spin': {
                '0%':   { transform: 'rotate(0deg)' },
                '100%': { transform: 'rotate(360deg)' },
              },
            }}
          >
            <RefreshIcon />
          </IconButton>
        </Tooltip>
      </Box>
    </Box>
  );
}
