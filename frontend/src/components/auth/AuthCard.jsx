/**
 * @fileoverview AuthCard — styled card container for auth forms.
 *
 * Provides consistent padding, border-radius, shadow, and header typography
 * across login and registration forms.
 */

import React from 'react';
import { Box, Card, CardContent, Typography } from '@mui/material';

/**
 * Auth form card wrapper.
 *
 * @param {{
 *   children:     React.ReactNode,
 *   title:        string,
 *   description?: string,
 * }} props
 * @returns {JSX.Element}
 */
export default function AuthCard({ children, title, description }) {
  return (
    <Card
      sx={{
        borderRadius: '20px',
        boxShadow: (theme) =>
          theme.palette.mode === 'dark'
            ? '0 16px 48px rgba(0,0,0,0.5)'
            : '0 16px 48px rgba(0,0,0,0.07)',
        border: '1px solid',
        borderColor: 'divider',
      }}
    >
      <CardContent sx={{ p: { xs: 3, sm: 4 }, '&:last-child': { pb: { xs: 3, sm: 4 } } }}>
        <Box sx={{ mb: 3.5 }}>
          <Typography variant="h4" fontWeight={800} sx={{ letterSpacing: '-0.01em', mb: 0.5 }}>
            {title}
          </Typography>
          {description && (
            <Typography variant="body2" color="text.secondary" sx={{ lineHeight: 1.6 }}>
              {description}
            </Typography>
          )}
        </Box>

        {children}
      </CardContent>
    </Card>
  );
}
