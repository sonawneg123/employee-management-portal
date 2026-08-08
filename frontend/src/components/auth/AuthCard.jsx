/**
 * @fileoverview AuthCard — the white card container that wraps every auth form.
 *
 * Provides consistent padding, border-radius, and shadow semantics
 * across the login and registration pages.
 */

import React from 'react';
import { Box, Card, CardContent, Typography } from '@mui/material';

/**
 * Styled card wrapper for authentication forms.
 *
 * @param {{
 *   children:    React.ReactNode,
 *   title:       string,
 *   description?: string,
 * }} props
 * @returns {JSX.Element}
 */
export default function AuthCard({ children, title, description }) {
  return (
    <Card
      sx={{
        borderRadius: 3,
        boxShadow: (theme) =>
          theme.palette.mode === 'dark'
            ? '0 8px 40px rgba(0,0,0,0.5)'
            : '0 8px 40px rgba(0,0,0,0.08)',
        border: '1px solid',
        borderColor: 'divider',
      }}
    >
      <CardContent sx={{ p: { xs: 3, sm: 4 }, '&:last-child': { pb: { xs: 3, sm: 4 } } }}>
        {/* Card header */}
        <Box sx={{ mb: 3 }}>
          <Typography variant="h5" fontWeight={700} gutterBottom>
            {title}
          </Typography>
          {description && (
            <Typography variant="body2" color="text.secondary">
              {description}
            </Typography>
          )}
        </Box>

        {children}
      </CardContent>
    </Card>
  );
}
