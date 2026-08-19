/**
 * @fileoverview PageHeader — reusable premium page heading component.
 *
 * Renders a consistent page title, subtitle, and optional action slot
 * used across all main content pages in PeopleCore HR.
 * Premium SaaS design — navy + gold accent.
 */

import React from 'react';
import { Box, Typography, useTheme } from '@mui/material';

/**
 * Standardised page header block.
 *
 * @param {{
 *   title:     string,
 *   subtitle?: string,
 *   action?:   React.ReactNode,
 *   icon?:     React.ReactNode,
 *   emoji?:    string,
 * }} props
 * @returns {JSX.Element}
 */
export default function PageHeader({ title, subtitle, action, icon, emoji }) {
  const theme = useTheme();
  const isDark = theme.palette.mode === 'dark';

  return (
    <Box
      sx={{
        mb: 3,
        display: 'flex',
        alignItems: { xs: 'flex-start', sm: 'center' },
        justifyContent: 'space-between',
        flexWrap: 'wrap',
        gap: 2,
      }}
    >
      <Box sx={{ display: 'flex', alignItems: 'center', gap: icon ? 1.5 : 0 }}>
        {icon && (
          <Box
            sx={{
              width: 46,
              height: 46,
              borderRadius: '14px',
              background: isDark ? 'rgba(245,197,24,0.12)' : 'rgba(26,35,66,0.06)',
              border: `1px solid ${isDark ? 'rgba(245,197,24,0.2)' : 'rgba(26,35,66,0.1)'}`,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              flexShrink: 0,
              color: isDark ? '#F5C518' : '#1A2342',
            }}
            aria-hidden="true"
          >
            {icon}
          </Box>
        )}
        <Box>
          <Typography
            variant="h2"
            fontWeight={800}
            sx={{
              letterSpacing: '-0.025em',
              mb: subtitle ? 0.25 : 0,
              lineHeight: 1.2,
              fontSize: { xs: '1.5rem', sm: '1.75rem' },
              color: isDark ? '#F0EDE6' : '#1A2342',
            }}
          >
            {emoji && (
              <Box component="span" sx={{ mr: 1 }} aria-hidden="true">
                {emoji}
              </Box>
            )}
            {title}
          </Typography>
          {subtitle && (
            <Typography
              variant="body2"
              sx={{ color: isDark ? 'rgba(240,237,230,0.5)' : '#9CA3AF' }}
            >
              {subtitle}
            </Typography>
          )}
        </Box>
      </Box>
      {action && <Box sx={{ flexShrink: 0 }}>{action}</Box>}
    </Box>
  );
}
