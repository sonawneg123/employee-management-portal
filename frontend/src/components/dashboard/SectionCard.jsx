/**
 * @fileoverview SectionCard — generic titled section container.
 *
 * Wraps dashboard sections (e.g., Recent Activity, Upcoming Leaves) with
 * a consistent card header including title, optional subtitle, optional
 * action slot, and the children content area.
 */

import React from 'react';
import {
  Box,
  Card,
  CardContent,
  CardHeader,
  Divider,
  IconButton,
  Tooltip,
  Typography,
} from '@mui/material';
import RefreshIcon from '@mui/icons-material/Refresh';

/**
 * @typedef {Object} SectionCardProps
 * @property {string}           title          - Card section title.
 * @property {string}           [subtitle]     - Optional subtitle below the title.
 * @property {React.ReactNode}  [action]       - Optional JSX rendered in the header action slot.
 * @property {boolean}          [showRefresh]  - Shows a refresh icon button if true.
 * @property {() => void}       [onRefresh]    - Callback for the refresh button.
 * @property {boolean}          [isFetching]   - Animates the refresh icon when true.
 * @property {React.ReactNode}  children       - Card body content.
 * @property {object}           [sx]           - Additional MUI sx overrides for the Card.
 * @property {object}           [contentSx]    - Additional MUI sx overrides for CardContent.
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
      <CardHeader
        title={
          <Typography variant="h6" fontWeight={600}>
            {title}
          </Typography>
        }
        subheader={subtitle && (
          <Typography variant="caption" color="text.secondary">
            {subtitle}
          </Typography>
        )}
        action={
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
            {action}
            {showRefresh && (
              <Tooltip title="Refresh">
                <IconButton
                  size="small"
                  onClick={onRefresh}
                  aria-label={`Refresh ${title}`}
                  sx={{
                    animation: isFetching ? 'spin 1s linear infinite' : 'none',
                    '@keyframes spin': { '0%': { transform: 'rotate(0deg)' }, '100%': { transform: 'rotate(360deg)' } },
                  }}
                >
                  <RefreshIcon fontSize="small" />
                </IconButton>
              </Tooltip>
            )}
          </Box>
        }
        sx={{ pb: 0 }}
      />
      <Divider sx={{ mx: 2, mt: 1 }} />
      <CardContent
        sx={{
          flexGrow: 1,
          p: 2,
          '&:last-child': { pb: 2 },
          ...contentSx,
        }}
      >
        {children}
      </CardContent>
    </Card>
  );
}
