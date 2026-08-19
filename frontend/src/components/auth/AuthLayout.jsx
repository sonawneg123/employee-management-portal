/**
 * @fileoverview AuthLayout — premium split-screen authentication layout.
 *
 * LEFT  (desktop): deep navy brand panel with gold accents, feature bullets, atmospheric blur.
 * RIGHT (all):     warm cream form panel with rounded card.
 *
 * Responsive: single column on mobile (form only, logo top).
 * Premium SaaS HR product aesthetic.
 */

import React from 'react';
import { Link as RouterLink } from 'react-router-dom';
import { Box, Grid, Typography, useMediaQuery, useTheme } from '@mui/material';
import PeopleAltRoundedIcon from '@mui/icons-material/PeopleAltRounded';
import EventAvailableRoundedIcon from '@mui/icons-material/EventAvailableRounded';
import AssessmentRoundedIcon from '@mui/icons-material/AssessmentRounded';
import AdminPanelSettingsRoundedIcon from '@mui/icons-material/AdminPanelSettingsRounded';
import AutoAwesomeRoundedIcon from '@mui/icons-material/AutoAwesomeRounded';

/** @type {{ icon: JSX.Element, label: string }[]} */
const FEATURES = [
  { icon: <PeopleAltRoundedIcon fontSize="small" />, label: 'Centralised employee management' },
  { icon: <EventAvailableRoundedIcon fontSize="small" />, label: 'Leave & attendance tracking' },
  { icon: <AssessmentRoundedIcon fontSize="small" />, label: 'Performance reviews & ratings' },
  { icon: <AdminPanelSettingsRoundedIcon fontSize="small" />, label: 'Role-based access control' },
  { icon: <AutoAwesomeRoundedIcon fontSize="small" />, label: 'Agentic AI Copilot' },
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
  const isDark = theme.palette.mode === 'dark';

  return (
    <Box
      sx={{
        minHeight: '100vh',
        background: isDark
          ? 'radial-gradient(ellipse at 10% 20%, rgba(26,35,66,0.8) 0%, transparent 60%), radial-gradient(ellipse at 90% 80%, rgba(20,28,55,0.6) 0%, transparent 60%), #0C1220'
          : 'radial-gradient(ellipse at 10% 0%, rgba(210,215,255,0.2) 0%, transparent 50%), radial-gradient(ellipse at 90% 100%, rgba(255,240,200,0.3) 0%, transparent 50%), #F5F0E8',
        display: 'flex',
      }}
    >
      <Grid container sx={{ flexGrow: 1 }}>
        {/* ── Left brand panel (desktop only) ─────────────────────────── */}
        {!isMobile && (
          <Grid
            size={5}
            sx={{
              background: 'linear-gradient(155deg, #0F1628 0%, #1A2342 45%, #2D3A6B 100%)',
              display: 'flex',
              flexDirection: 'column',
              justifyContent: 'center',
              p: { md: 5, lg: 7 },
              position: 'relative',
              overflow: 'hidden',
            }}
          >
            {/* Decorative atmospheric blobs */}
            <Box
              aria-hidden="true"
              sx={{
                position: 'absolute',
                width: 400,
                height: 400,
                borderRadius: '50%',
                background: 'radial-gradient(circle, rgba(245,197,24,0.08) 0%, transparent 70%)',
                top: -100,
                right: -100,
                pointerEvents: 'none',
              }}
            />
            <Box
              aria-hidden="true"
              sx={{
                position: 'absolute',
                width: 280,
                height: 280,
                borderRadius: '50%',
                background: 'radial-gradient(circle, rgba(79,106,181,0.2) 0%, transparent 70%)',
                bottom: -80,
                left: -80,
                pointerEvents: 'none',
              }}
            />
            {/* Top right decorative dot grid */}
            <Box
              aria-hidden="true"
              sx={{
                position: 'absolute',
                top: 40,
                right: 40,
                width: 80,
                height: 80,
                opacity: 0.15,
                backgroundImage:
                  'radial-gradient(circle, rgba(245,197,24,0.8) 1px, transparent 1px)',
                backgroundSize: '12px 12px',
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
                  width: 40,
                  height: 40,
                  borderRadius: '11px',
                  background: 'linear-gradient(135deg, #F5C518, #C49A00)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  flexShrink: 0,
                  boxShadow: '0 4px 16px rgba(245,197,24,0.4)',
                }}
              >
                <PeopleAltRoundedIcon sx={{ color: '#1A2342', fontSize: 22 }} />
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
                lineHeight: 1.15,
                letterSpacing: '-0.025em',
              }}
            >
              {title ?? 'Manage your workforce smarter'}
            </Typography>

            {subtitle && (
              <Typography
                variant="body1"
                sx={{
                  color: 'rgba(255,255,255,0.65)',
                  mb: 5,
                  maxWidth: 340,
                  lineHeight: 1.7,
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
                      bgcolor: 'rgba(245,197,24,0.12)',
                      border: '1px solid rgba(245,197,24,0.2)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      color: '#F5C518',
                      flexShrink: 0,
                    }}
                  >
                    {icon}
                  </Box>
                  <Typography
                    variant="body2"
                    sx={{ color: 'rgba(255,255,255,0.8)', fontWeight: 500 }}
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
                  width: 36,
                  height: 36,
                  borderRadius: '10px',
                  background: 'linear-gradient(135deg, #F5C518, #C49A00)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  boxShadow: '0 4px 12px rgba(245,197,24,0.35)',
                }}
              >
                <PeopleAltRoundedIcon sx={{ color: '#1A2342', fontSize: 20 }} />
              </Box>
              <Typography
                variant="h6"
                fontWeight={800}
                sx={{ color: isDark ? '#F0EDE6' : '#1A2342', letterSpacing: '-0.02em' }}
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
