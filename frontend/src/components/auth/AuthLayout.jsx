/**
 * @fileoverview AuthLayout — full-page layout wrapper for authentication pages.
 *
 * Renders a responsive two-column layout:
 * - Left panel: brand illustration / feature highlights (hidden on mobile).
 * - Right panel: the auth form card passed as children.
 *
 * Uses the MUI theme exclusively — no inline CSS values.
 */

import React from 'react';
import { Link as RouterLink } from 'react-router-dom';
import {
  Box,
  Grid,
  Typography,
  useMediaQuery,
  useTheme,
} from '@mui/material';
import PeopleAltIcon      from '@mui/icons-material/PeopleAlt';
import AssignmentIndIcon  from '@mui/icons-material/AssignmentInd';
import EventAvailableIcon from '@mui/icons-material/EventAvailable';
import BarChartIcon       from '@mui/icons-material/BarChart';

/**
 * @typedef {Object} FeatureItem
 * @property {JSX.Element} icon  - MUI icon element.
 * @property {string}      label - Short feature description.
 */

/** @type {FeatureItem[]} */
const FEATURES = [
  { icon: <PeopleAltIcon />,      label: 'Manage your entire workforce from one place' },
  { icon: <AssignmentIndIcon />,  label: 'Role-based access for Admins, HR, and Managers' },
  { icon: <EventAvailableIcon />, label: 'Track attendance and leave requests in real time' },
  { icon: <BarChartIcon />,       label: 'Conduct and review performance evaluations' },
];

/**
 * Full-page authentication layout.
 *
 * @param {{
 *   children: React.ReactNode,
 *   title?: string,
 *   subtitle?: string,
 * }} props
 * @returns {JSX.Element}
 */
export default function AuthLayout({ children, title, subtitle }) {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));

  return (
    <Box
      sx={{
        minHeight: '100vh',
        bgcolor: 'background.default',
        display: 'flex',
      }}
    >
      <Grid container sx={{ flexGrow: 1 }}>
        {/* ── Left branding panel (desktop only) ── */}
        {!isMobile && (
          <Grid
            size={5}
            sx={{
              bgcolor: 'primary.main',
              display: 'flex',
              flexDirection: 'column',
              justifyContent: 'center',
              p: 6,
              position: 'relative',
              overflow: 'hidden',
              '&::after': {
                content: '""',
                position: 'absolute',
                width: 400,
                height: 400,
                borderRadius: '50%',
                bgcolor: 'rgba(255,255,255,0.06)',
                bottom: -80,
                right: -80,
              },
            }}
          >
            {/* Logo */}
            <Box sx={{ mb: 6 }}>
              <Typography
                component={RouterLink}
                to="/"
                variant="h5"
                fontWeight={800}
                sx={{ color: 'primary.contrastText', textDecoration: 'none' }}
              >
                EMP Portal
              </Typography>
            </Box>

            {/* Hero copy */}
            <Typography
              variant="h3"
              fontWeight={700}
              sx={{ color: 'primary.contrastText', mb: 2, lineHeight: 1.2 }}
            >
              {title ?? 'Manage your workforce smarter'}
            </Typography>

            {subtitle && (
              <Typography
                variant="body1"
                sx={{ color: 'rgba(255,255,255,0.8)', mb: 5, maxWidth: 360 }}
              >
                {subtitle}
              </Typography>
            )}

            {/* Feature list */}
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2.5 }}>
              {FEATURES.map(({ icon, label }) => (
                <Box key={label} sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                  <Box
                    sx={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      width: 40,
                      height: 40,
                      borderRadius: 2,
                      bgcolor: 'rgba(255,255,255,0.15)',
                      color: 'primary.contrastText',
                      flexShrink: 0,
                    }}
                  >
                    {React.cloneElement(icon, { fontSize: 'small' })}
                  </Box>
                  <Typography variant="body2" sx={{ color: 'rgba(255,255,255,0.9)' }}>
                    {label}
                  </Typography>
                </Box>
              ))}
            </Box>
          </Grid>
        )}

        {/* ── Right form panel ── */}
        <Grid
          size={{ xs: 12, md: 7 }}
          sx={{
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            p: { xs: 3, sm: 6 },
          }}
        >
          {/* Mobile logo */}
          {isMobile && (
            <Typography
              component={RouterLink}
              to="/"
              variant="h5"
              fontWeight={800}
              color="primary.main"
              sx={{ textDecoration: 'none', mb: 4 }}
            >
              EMP Portal
            </Typography>
          )}

          <Box sx={{ width: '100%', maxWidth: 460 }}>
            {children}
          </Box>
        </Grid>
      </Grid>
    </Box>
  );
}
