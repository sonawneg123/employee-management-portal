/**
 * @fileoverview AuthCard — premium styled card container for auth forms.
 *
 * Provides consistent padding, border-radius, shadow, and header typography
 * across login and registration forms. Deep navy title, gold accent, warm card.
 */

import React from 'react';
import { Box, Card, CardContent, Typography, useTheme } from '@mui/material';

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
  const theme = useTheme();
  const isDark = theme.palette.mode === 'dark';

  return (
    <Card
      sx={{
        borderRadius: '24px',
        boxShadow: isDark ? '0 20px 60px rgba(0,0,0,0.6)' : '0 20px 60px rgba(26,35,66,0.1)',
        border: `1px solid ${isDark ? 'rgba(240,237,230,0.07)' : '#EBE6DA'}`,
        bgcolor: isDark ? '#131C2E' : '#FFFFFF',
      }}
    >
      <CardContent sx={{ p: { xs: 3, sm: 4 }, '&:last-child': { pb: { xs: 3, sm: 4 } } }}>
        <Box sx={{ mb: 3.5 }}>
          {/* Gold accent line */}
          <Box
            sx={{
              width: 36,
              height: 3,
              borderRadius: 999,
              background: 'linear-gradient(90deg, #F5C518, #C49A00)',
              mb: 2,
            }}
            aria-hidden="true"
          />
          <Typography
            variant="h4"
            fontWeight={800}
            sx={{
              letterSpacing: '-0.02em',
              mb: 0.75,
              color: isDark ? '#F0EDE6' : '#1A2342',
            }}
          >
            {title}
          </Typography>
          {description && (
            <Typography variant="body2" color="text.secondary" sx={{ lineHeight: 1.65 }}>
              {description}
            </Typography>
          )}
        </Box>

        {children}
      </CardContent>
    </Card>
  );
}
