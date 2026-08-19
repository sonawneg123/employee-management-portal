/**
 * @fileoverview PageHeader — reusable modern page heading component.
 *
 * Renders a consistent page title, subtitle, and optional action slot
 * used across all main content pages in PeopleCore HR.
 */

import React from 'react';
import { Box, Typography } from '@mui/material';

/**
 * Standardised page header block.
 *
 * @param {{
 *   title:     string,
 *   subtitle?: string,
 *   action?:   React.ReactNode,
 *   icon?:     React.ReactNode,
 * }} props
 * @returns {JSX.Element}
 */
export default function PageHeader({ title, subtitle, action, icon, emoji }) {
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
              width: 44,
              height: 44,
              borderRadius: '12px',
              background: 'linear-gradient(135deg, rgba(79,70,229,0.12), rgba(124,58,237,0.08))',
              border: '1px solid rgba(79,70,229,0.15)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              flexShrink: 0,
              color: 'primary.main',
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
            <Typography variant="body2" color="text.secondary">
              {subtitle}
            </Typography>
          )}
        </Box>
      </Box>
      {action && <Box sx={{ flexShrink: 0 }}>{action}</Box>}
    </Box>
  );
}
