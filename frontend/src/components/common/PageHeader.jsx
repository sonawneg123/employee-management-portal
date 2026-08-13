/**
 * @fileoverview PageHeader — reusable premium page heading component.
 *
 * Renders a consistent page title, subtitle, and optional action slot
 * used across all main content pages.
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
 *   emoji?:    string,
 * }} props
 * @returns {JSX.Element}
 */
export default function PageHeader({ title, subtitle, action, emoji }) {
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
      <Box>
        <Typography
          variant="h2"
          fontWeight={800}
          sx={{ letterSpacing: '-0.02em', mb: subtitle ? 0.25 : 0, lineHeight: 1.2 }}
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
      {action && <Box sx={{ flexShrink: 0 }}>{action}</Box>}
    </Box>
  );
}
