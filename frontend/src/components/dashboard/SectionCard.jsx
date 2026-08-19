/**
 * @fileoverview SectionCard — premium titled section container.
 *
 * Wraps dashboard widgets with a consistent card header, optional refresh,
 * and children content area. Used throughout dashboard and management pages.
 * Premium SaaS design — clean, spacious, navy + gold accent.
 */

import React from 'react';
import {
  Box,
  Card,
  CardContent,
  Divider,
  IconButton,
  Tooltip,
  Typography,
  useTheme,
} from '@mui/material';
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
  const theme = useTheme();
  const isDark = theme.palette.mode === 'dark';

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
            sx={{
              letterSpacing: '-0.01em',
              lineHeight: 1.3,
              color: isDark ? '#F0EDE6' : '#1A2342',
            }}
          >
            {title}
          </Typography>
          {subtitle && (
            <Typography
              variant="caption"
              sx={{
                mt: 0.25,
                display: 'block',
                color: isDark ? 'rgba(240,237,230,0.45)' : '#9CA3AF',
              }}
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
                  bgcolor: isDark ? 'rgba(255,255,255,0.06)' : 'rgba(26,35,66,0.04)',
                  borderRadius: '8px',
                  width: 30,
                  height: 30,
                  color: isDark ? 'rgba(240,237,230,0.5)' : '#9CA3AF',
                  animation: isFetching ? 'spin 1s linear infinite' : 'none',
                  '@keyframes spin': {
                    '0%': { transform: 'rotate(0deg)' },
                    '100%': { transform: 'rotate(360deg)' },
                  },
                  '&:hover': {
                    bgcolor: isDark ? 'rgba(245,197,24,0.12)' : 'rgba(26,35,66,0.07)',
                    color: isDark ? '#F5C518' : '#1A2342',
                  },
                  transition: 'all 0.15s ease',
                }}
              >
                <RefreshRoundedIcon sx={{ fontSize: 14 }} />
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
