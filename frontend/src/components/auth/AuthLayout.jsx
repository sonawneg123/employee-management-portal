/**
 * @fileoverview AuthLayout — premium split-screen authentication layout.
 *
 * LEFT  (desktop): brand panel with gradient, feature bullets, animated accents.
 * RIGHT (all):     form panel with card content.
 *
 * Responsive: single column on mobile (form only, logo top).
 */

import React from 'react';
import { Link as RouterLink } from 'react-router-dom';
import { Box, Grid, Typography, useMediaQuery, useTheme } from '@mui/material';
import PeopleAltIcon from '@mui/icons-material/PeopleAlt';
import EventAvailableIcon from '@mui/icons-material/EventAvailable';
import AssessmentIcon from '@mui/icons-material/Assessment';
import AdminPanelSettingsIcon from '@mui/icons-material/AdminPanelSettings';

/** @type {{ icon: JSX.Element, label: string }[]} */
const FEATURES = [
  { icon: <PeopleAltIcon fontSize="small" />, label: 'Centralised employee management' },
  { icon: <EventAvailableIcon fontSize="small" />, label: 'Leave & attendance tracking' },
  { icon: <AssessmentIcon fontSize="small" />, label: 'Performance reviews & ratings' },
  { icon: <AdminPanelSettingsIcon fontSize="small" />, label: 'Role-based access control' },
];

/**
 * Full-page authentication layout.
 *
 * @param {{
 *   children:   React.ReactNode,
 *   title?:     string,
 *   subtitle?:  string,
 * }} props
 * @returns {JSX.Element}
 */
export default function AuthLayout({ children, title, subtitle }) {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  // isDark intentionally read via theme in sx callbacks

  return (
    <Box
      sx={{
        minHeight: '100vh',
        bgcolor: 'background.default',
        display: 'flex',
      }}
    >
      <Grid container sx={{ flexGrow: 1 }}>
        {/* ── Left brand panel (desktop only) ─────────────────────────── */}
        {!isMobile && (
          <Grid
            size={5}
            sx={{
              background: 'linear-gradient(145deg, #3730A3 0%, #4F46E5 40%, #7C3AED 100%)',
              display: 'flex',
              flexDirection: 'column',
              justifyContent: 'center',
              p: { md: 5, lg: 7 },
              position: 'relative',
              overflow: 'hidden',
            }}
          >
            {/* Decorative blobs */}
            <Box
              aria-hidden="true"
              sx={{
                position: 'absolute',
                width: 340,
                height: 340,
                borderRadius: '50%',
                background: 'rgba(255,255,255,0.05)',
                top: -80,
                right: -80,
                pointerEvents: 'none',
              }}
            />
            <Box
              aria-hidden="true"
              sx={{
                position: 'absolute',
                width: 220,
                height: 220,
                borderRadius: '50%',
                background: 'rgba(255,255,255,0.04)',
                bottom: -60,
                left: -60,
                pointerEvents: 'none',
              }}
            />

            {/* Logo */}
            <Box
              component={RouterLink}
              to="/"
              sx={{
                display: 'flex',
                alignItems: 'center',
                gap: 1.25,
                textDecoration: 'none',
                mb: 7,
              }}
            >
              <Box
                sx={{
                  width: 36,
                  height: 36,
                  borderRadius: '9px',
                  bgcolor: 'rgba(255,255,255,0.2)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  backdropFilter: 'blur(4px)',
                }}
              >
                <PeopleAltIcon sx={{ color: '#fff', fontSize: 20 }} />
              </Box>
              <Typography
                variant="h6"
                fontWeight={800}
                sx={{ color: '#fff', letterSpacing: '-0.02em' }}
              >
                PeopleCore HR
              </Typography>
            </Box>

            {/* Hero copy */}
            <Typography
              variant="h2"
              fontWeight={800}
              sx={{
                color: '#fff',
                mb: 1.5,
                lineHeight: 1.2,
                letterSpacing: '-0.025em',
              }}
            >
              {title ?? 'Manage your workforce smarter'}
            </Typography>

            {subtitle && (
              <Typography
                variant="body1"
                sx={{
                  color: 'rgba(255,255,255,0.75)',
                  mb: 5,
                  maxWidth: 340,
                  lineHeight: 1.65,
                }}
              >
                {subtitle}
              </Typography>
            )}

            {/* Feature bullets */}
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
              {FEATURES.map(({ icon, label }) => (
                <Box key={label} sx={{ display: 'flex', alignItems: 'center', gap: 1.75 }}>
                  <Box
                    sx={{
                      width: 34,
                      height: 34,
                      borderRadius: '9px',
                      bgcolor: 'rgba(255,255,255,0.15)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      color: '#fff',
                      flexShrink: 0,
                    }}
                  >
                    {icon}
                  </Box>
                  <Typography
                    variant="body2"
                    sx={{ color: 'rgba(255,255,255,0.88)', fontWeight: 500 }}
                  >
                    {label}
                  </Typography>
                </Box>
              ))}
            </Box>
          </Grid>
        )}

        {/* ── Right form panel ─────────────────────────────────────────── */}
        <Grid
          size={{ xs: 12, md: 7 }}
          sx={{
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            p: { xs: 3, sm: 5 },
            animation: 'scaleIn 0.3s ease',
          }}
        >
          {/* Mobile logo */}
          {isMobile && (
            <Box
              component={RouterLink}
              to="/"
              sx={{
                display: 'flex',
                alignItems: 'center',
                gap: 1,
                textDecoration: 'none',
                mb: 4,
              }}
            >
              <Box
                sx={{
                  width: 32,
                  height: 32,
                  borderRadius: '8px',
                  bgcolor: 'primary.main',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                }}
              >
                <PeopleAltIcon sx={{ color: '#fff', fontSize: 18 }} />
              </Box>
              <Typography
                variant="h6"
                fontWeight={800}
                sx={{ color: 'text.primary', letterSpacing: '-0.02em' }}
              >
                PeopleCore HR
              </Typography>
            </Box>
          )}

          <Box sx={{ width: '100%', maxWidth: 460 }}>{children}</Box>
        </Grid>
      </Grid>
    </Box>
  );
}
