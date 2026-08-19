/**
 * @fileoverview SectionCard — premium titled section container.
 *
 * Wraps dashboard widgets with a consistent card header, optional refresh,
 * and children content area. Used throughout dashboard and management pages.
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
          px: 3,
          pt: 2.5,
          pb: 1.5,
          display: 'flex',
          alignItems: 'flex-start',
          justifyContent: 'space-between',
          gap: 1,
        }}
      >
        <Box sx={{ minWidth: 0 }}>
          <Typography
            variant="h5"
            fontWeight={700}
            sx={{ letterSpacing: '-0.01em', lineHeight: 1.3 }}
          >
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
                  '&:hover': { bgcolor: 'rgba(79,70,229,0.08)', color: 'primary.main' },
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
          p: 3,
          '&:last-child': { pb: 3 },
          ...contentSx,
        }}
      >
        {children}
      </CardContent>
    </Card>
  );
}
