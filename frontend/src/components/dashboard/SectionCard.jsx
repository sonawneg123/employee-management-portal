/**
 * @fileoverview SectionCard — premium titled section container.
 *
 * Wraps dashboard widgets with a consistent card header, optional refresh,
 * and children content area.
 */

import React from 'react';
import { Box, Card, CardContent, Divider, IconButton, Tooltip, Typography } from '@mui/material';
import RefreshRoundedIcon from '@mui/icons-material/RefreshRounded';

/**
 * @typedef {Object} SectionCardProps
 * @property {string}          title
 * @property {string}          [subtitle]
 * @property {React.ReactNode} [action]
 * @property {boolean}         [showRefresh]
 * @property {() => void}      [onRefresh]
 * @property {boolean}         [isFetching]
 * @property {React.ReactNode} children
 * @property {object}          [sx]
 * @property {object}          [contentSx]
 */

/**
 * Titled section card for dashboard widgets.
 *
 * @param {SectionCardProps} props
 * @returns {JSX.Element}
 */
export default function SectionCard({
  title,
  subtitle,
  action,
  showRefresh = false,
  onRefresh,
  isFetching = false,
  children,
  sx,
  contentSx,
}) {
  return (
    <Card sx={{ height: '100%', display: 'flex', flexDirection: 'column', ...sx }}>
      {/* Header */}
      <Box
        sx={{
          px: 2.5,
          pt: 2.5,
          pb: 1.5,
          display: 'flex',
          alignItems: 'flex-start',
          justifyContent: 'space-between',
          gap: 1,
        }}
      >
        <Box>
          <Typography variant="h5" fontWeight={700} sx={{ letterSpacing: '-0.005em' }}>
            {title}
          </Typography>
          {subtitle && (
            <Typography
              variant="caption"
              color="text.secondary"
              sx={{ mt: 0.25, display: 'block' }}
            >
              {subtitle}
            </Typography>
          )}
        </Box>

        <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, flexShrink: 0 }}>
          {action}
          {showRefresh && (
            <Tooltip title={`Refresh ${title}`}>
              <IconButton
                size="small"
                onClick={onRefresh}
                aria-label={`Refresh ${title}`}
                sx={{
                  bgcolor: 'action.hover',
                  borderRadius: '8px',
                  width: 30,
                  height: 30,
                  animation: isFetching ? 'spin 1s linear infinite' : 'none',
                  '@keyframes spin': {
                    '0%': { transform: 'rotate(0deg)' },
                    '100%': { transform: 'rotate(360deg)' },
                  },
                }}
              >
                <RefreshRoundedIcon sx={{ fontSize: 15 }} />
              </IconButton>
            </Tooltip>
          )}
        </Box>
      </Box>

      <Divider />

      <CardContent
        sx={{
          flexGrow: 1,
          p: 2.5,
          '&:last-child': { pb: 2.5 },
          ...contentSx,
        }}
      >
        {children}
      </CardContent>
    </Card>
  );
}
