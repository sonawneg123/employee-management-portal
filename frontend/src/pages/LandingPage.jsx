/**
 * @fileoverview LandingPage — home page with explicit auth option cards.
 *
 * Displays all available sign-in and registration paths without hiding them
 * inside menus. Sign-In section shows three role-specific login buttons;
 * Create Account section shows HR and Employee registration buttons.
 * Admin account creation is intentionally not available publicly.
 */

import React from 'react';
import { Link as RouterLink } from 'react-router-dom';
import { Helmet } from 'react-helmet-async';
import { Box, Button, Container, Grid, Typography, useTheme, Divider, Chip } from '@mui/material';
import PeopleAltIcon from '@mui/icons-material/PeopleAlt';
import AdminPanelSettingsIcon from '@mui/icons-material/AdminPanelSettings';
import WorkIcon from '@mui/icons-material/Work';
import PersonIcon from '@mui/icons-material/Person';
import PersonAddIcon from '@mui/icons-material/PersonAdd';
import EventAvailableIcon from '@mui/icons-material/EventAvailable';
import BarChartIcon from '@mui/icons-material/BarChart';
import AccessTimeIcon from '@mui/icons-material/AccessTime';
import AssessmentIcon from '@mui/icons-material/Assessment';
import { ROUTES } from '@/constants/routes';

/** Feature data used in the feature grid. */
const FEATURES = [
  {
    icon: <PeopleAltIcon />,
    title: 'Employee Directory',
    description: 'Centralised records for every team member — hire to retire.',
    color: '#4F46E5',
  },
  {
    icon: <EventAvailableIcon />,
    title: 'Leave Management',
    description: 'Submit, approve, and track leave requests in real time.',
    color: '#7C3AED',
  },
  {
    icon: <AccessTimeIcon />,
    title: 'Attendance Tracking',
    description: 'Monitor attendance patterns and generate daily presence reports.',
    color: '#10B981',
  },
  {
    icon: <BarChartIcon />,
    title: 'Department Analytics',
    description: 'Visualise headcount distribution and workforce metrics.',
    color: '#F59E0B',
  },
  {
    icon: <AssessmentIcon />,
    title: 'Performance Reviews',
    description: 'Structured evaluation cycles with rating history.',
    color: '#3B82F6',
  },
  {
    icon: <AdminPanelSettingsIcon />,
    title: 'Role-Based Access',
    description: 'Granular permissions for Admin, HR, Manager, and Employee.',
    color: '#EF4444',
  },
];

/** Sign-in options shown in the "Sign In" section. */
const LOGIN_OPTIONS = [
  {
    icon: <AdminPanelSettingsIcon />,
    label: 'Login as Admin',
    description: 'System administration & user management',
    to: ROUTES.LOGIN_ADMIN,
    color: '#EF4444',
    bg: 'rgba(239,68,68,0.08)',
  },
  {
    icon: <WorkIcon />,
    label: 'Login as HR',
    description: 'HR operations, employees & leave approvals',
    to: ROUTES.LOGIN_HR,
    color: '#4F46E5',
    bg: 'rgba(79,70,229,0.08)',
  },
  {
    icon: <PersonIcon />,
    label: 'Login as Employee',
    description: 'Self-service dashboard, leaves & attendance',
    to: ROUTES.LOGIN_EMPLOYEE,
    color: '#10B981',
    bg: 'rgba(16,185,129,0.08)',
  },
];

/** Create-account options shown in the "Create Account" section. */
const REGISTER_OPTIONS = [
  {
    icon: <WorkIcon />,
    label: 'Create HR Account',
    description: 'Register a new HR portal account',
    to: ROUTES.REGISTER_HR,
    color: '#4F46E5',
    bg: 'rgba(79,70,229,0.08)',
    variant: 'contained',
  },
  {
    icon: <PersonAddIcon />,
    label: 'Create Employee Account',
    description: 'Register as a new employee',
    to: ROUTES.REGISTER_EMPLOYEE,
    color: '#10B981',
    bg: 'rgba(16,185,129,0.08)',
    variant: 'outlined',
  },
];

/**
 * Auth option card used for both sign-in and register options.
 *
 * @param {{ icon: JSX.Element, label: string, description: string, to: string, color: string, bg: string, contained?: boolean }} props
 */
function AuthOptionCard({ icon, label, description, to, color, bg, contained }) {
  const theme = useTheme();
  const isDark = theme.palette.mode === 'dark';
  return (
    <Box
      component={RouterLink}
      to={to}
      sx={{
        display: 'flex',
        alignItems: 'center',
        gap: 2,
        p: 2,
        borderRadius: '14px',
        border: '1px solid',
        borderColor: contained ? color : 'divider',
        bgcolor: contained ? (isDark ? `${color}22` : bg) : 'background.paper',
        textDecoration: 'none',
        transition: 'box-shadow 0.2s ease, transform 0.2s ease, border-color 0.2s ease',
        '&:hover': {
          boxShadow: isDark ? `0 6px 20px rgba(0,0,0,0.4)` : `0 6px 20px ${color}22`,
          transform: 'translateY(-1px)',
          borderColor: color,
        },
      }}
    >
      <Box
        sx={{
          width: 44,
          height: 44,
          borderRadius: '11px',
          bgcolor: bg,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          flexShrink: 0,
        }}
      >
        {React.cloneElement(icon, { sx: { color, fontSize: 22 } })}
      </Box>
      <Box sx={{ minWidth: 0 }}>
        <Typography variant="body1" fontWeight={700} color="text.primary" noWrap>
          {label}
        </Typography>
        <Typography variant="caption" color="text.secondary" display="block">
          {description}
        </Typography>
      </Box>
      <Box sx={{ ml: 'auto', color: 'text.disabled', fontSize: 18 }}>›</Box>
    </Box>
  );
}

/**
 * Landing / home page.
 *
 * @returns {JSX.Element}
 */
export default function LandingPage() {
  const theme = useTheme();
  const isDark = theme.palette.mode === 'dark';

  return (
    <>
      <Helmet>
        <title>PeopleCore HR — Modern Employee Management</title>
        <meta
          name="description"
          content="Enterprise HR management platform. Manage employees, leaves, attendance and performance in one place."
        />
      </Helmet>

      <Box
        sx={{
          minHeight: '100vh',
          bgcolor: 'background.default',
          display: 'flex',
          flexDirection: 'column',
          animation: 'fadeIn 0.4s ease',
        }}
      >
        {/* ── Navigation bar ───────────────────────────────────────────────── */}
        <Box
          component="nav"
          sx={{
            borderBottom: '1px solid',
            borderColor: 'divider',
            bgcolor: 'background.paper',
            position: 'sticky',
            top: 0,
            zIndex: 10,
          }}
        >
          <Container maxWidth="lg">
            <Box
              sx={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                py: 2,
              }}
            >
              {/* Logo */}
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
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
                  sx={{ letterSpacing: '-0.02em', color: 'text.primary' }}
                >
                  PeopleCore
                  <Typography component="span" color="primary.main" fontWeight={800}>
                    {' '}
                    HR
                  </Typography>
                </Typography>
              </Box>

              {/* Quick nav */}
              <Box sx={{ display: 'flex', gap: 1.5, alignItems: 'center' }}>
                <Button
                  component={RouterLink}
                  to={ROUTES.LOGIN_EMPLOYEE}
                  variant="text"
                  color="inherit"
                  size="small"
                  sx={{
                    fontWeight: 600,
                    color: 'text.secondary',
                    display: { xs: 'none', sm: 'inline-flex' },
                  }}
                >
                  Sign In
                </Button>
                <Button
                  component={RouterLink}
                  to={ROUTES.REGISTER_EMPLOYEE}
                  variant="contained"
                  size="small"
                  sx={{ fontWeight: 600 }}
                >
                  Get Started
                </Button>
              </Box>
            </Box>
          </Container>
        </Box>

        {/* ── Hero section ─────────────────────────────────────────────────── */}
        <Box
          sx={{
            pt: { xs: 6, md: 10 },
            pb: { xs: 4, md: 6 },
          }}
        >
          <Container maxWidth="lg">
            <Box sx={{ textAlign: 'center', mb: { xs: 6, md: 8 } }}>
              <Chip
                label="Employee Management Portal"
                size="small"
                sx={{
                  mb: 3,
                  bgcolor: 'rgba(79,70,229,0.1)',
                  color: 'primary.main',
                  fontWeight: 700,
                  border: '1px solid rgba(79,70,229,0.2)',
                }}
              />
              <Typography
                variant="h1"
                sx={{
                  fontSize: { xs: '2.25rem', md: '3rem' },
                  fontWeight: 800,
                  lineHeight: 1.15,
                  letterSpacing: '-0.03em',
                  mb: 2,
                  animation: 'fadeUp 0.5s ease 0.1s both',
                }}
              >
                Manage people.{' '}
                <Typography
                  component="span"
                  sx={{
                    fontSize: 'inherit',
                    fontWeight: 'inherit',
                    lineHeight: 'inherit',
                    letterSpacing: 'inherit',
                    background: 'linear-gradient(135deg, #4F46E5 0%, #7C3AED 100%)',
                    WebkitBackgroundClip: 'text',
                    WebkitTextFillColor: 'transparent',
                    backgroundClip: 'text',
                  }}
                >
                  Simplify HR.
                </Typography>{' '}
                Empower employees.
              </Typography>
              <Typography
                variant="body1"
                color="text.secondary"
                sx={{
                  fontSize: { xs: '1rem', md: '1.0625rem' },
                  lineHeight: 1.7,
                  maxWidth: 520,
                  mx: 'auto',
                  animation: 'fadeUp 0.5s ease 0.2s both',
                }}
              >
                Streamline employee management, attendance tracking, and performance reviews. Built
                for HR teams that demand clarity and control.
              </Typography>
            </Box>

            {/* ── Auth options grid ─────────────────────────────────────────── */}
            <Grid container spacing={4} justifyContent="center">
              {/* Sign In section */}
              <Grid size={{ xs: 12, md: 5 }}>
                <Box
                  sx={{
                    p: 3,
                    borderRadius: '20px',
                    border: '1px solid',
                    borderColor: 'divider',
                    bgcolor: 'background.paper',
                    boxShadow: isDark
                      ? '0 8px 32px rgba(0,0,0,0.4)'
                      : '0 8px 32px rgba(79,70,229,0.07)',
                    animation: 'fadeUp 0.5s ease 0.3s both',
                  }}
                >
                  <Typography
                    variant="overline"
                    color="primary.main"
                    sx={{ letterSpacing: '0.12em', fontWeight: 700, mb: 0.5, display: 'block' }}
                  >
                    Sign In
                  </Typography>
                  <Typography variant="h6" fontWeight={700} sx={{ mb: 2.5 }}>
                    Choose your role to sign in
                  </Typography>
                  <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
                    {LOGIN_OPTIONS.map((opt) => (
                      <AuthOptionCard key={opt.to} {...opt} />
                    ))}
                  </Box>
                </Box>
              </Grid>

              {/* Divider */}
              <Grid
                size={{ xs: 12, md: 'auto' }}
                sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center' }}
              >
                <Divider
                  orientation="vertical"
                  flexItem
                  sx={{ display: { xs: 'none', md: 'block' } }}
                />
                <Divider sx={{ display: { xs: 'block', md: 'none' }, width: '100%' }} />
              </Grid>

              {/* Create Account section */}
              <Grid size={{ xs: 12, md: 5 }}>
                <Box
                  sx={{
                    p: 3,
                    borderRadius: '20px',
                    border: '1px solid',
                    borderColor: 'divider',
                    bgcolor: 'background.paper',
                    boxShadow: isDark
                      ? '0 8px 32px rgba(0,0,0,0.4)'
                      : '0 8px 32px rgba(79,70,229,0.07)',
                    animation: 'fadeUp 0.5s ease 0.4s both',
                    height: '100%',
                  }}
                >
                  <Typography
                    variant="overline"
                    color="primary.main"
                    sx={{ letterSpacing: '0.12em', fontWeight: 700, mb: 0.5, display: 'block' }}
                  >
                    Create Account
                  </Typography>
                  <Typography variant="h6" fontWeight={700} sx={{ mb: 2.5 }}>
                    New to PeopleCore?
                  </Typography>
                  <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
                    {REGISTER_OPTIONS.map((opt) => (
                      <AuthOptionCard
                        key={opt.to}
                        {...opt}
                        contained={opt.variant === 'contained'}
                      />
                    ))}
                  </Box>
                  <Typography
                    variant="caption"
                    color="text.disabled"
                    sx={{ display: 'block', mt: 2.5, textAlign: 'center' }}
                  >
                    Admin accounts are managed by system administrators.
                  </Typography>
                </Box>
              </Grid>
            </Grid>
          </Container>
        </Box>

        {/* ── Feature highlights ────────────────────────────────────────────── */}
        <Box
          sx={{
            py: { xs: 6, md: 10 },
            borderTop: '1px solid',
            borderColor: 'divider',
            bgcolor: isDark ? 'rgba(255,255,255,0.015)' : 'rgba(79,70,229,0.02)',
          }}
        >
          <Container maxWidth="lg">
            <Box sx={{ textAlign: 'center', mb: { xs: 4, md: 6 } }}>
              <Typography
                variant="overline"
                color="primary.main"
                sx={{ letterSpacing: '0.15em', mb: 1, display: 'block' }}
              >
                What&apos;s included
              </Typography>
              <Typography variant="h2" fontWeight={800} sx={{ letterSpacing: '-0.02em', mb: 1.5 }}>
                Everything you need to manage people
              </Typography>
              <Typography variant="body1" color="text.secondary" sx={{ maxWidth: 540, mx: 'auto' }}>
                A complete HR toolkit — from onboarding to reviews — with role-based access that
                keeps every stakeholder in their lane.
              </Typography>
            </Box>

            <Grid container spacing={3}>
              {FEATURES.map(({ icon, title, description, color }) => (
                <Grid key={title} size={{ xs: 12, sm: 6, md: 4 }}>
                  <Box
                    sx={{
                      p: 3,
                      borderRadius: '16px',
                      border: '1px solid',
                      borderColor: 'divider',
                      bgcolor: 'background.paper',
                      height: '100%',
                      transition: 'box-shadow 0.2s ease, transform 0.2s ease',
                      '&:hover': {
                        boxShadow: isDark
                          ? '0 8px 24px rgba(0,0,0,0.4)'
                          : '0 8px 24px rgba(0,0,0,0.08)',
                        transform: 'translateY(-2px)',
                      },
                    }}
                  >
                    <Box
                      sx={{
                        width: 44,
                        height: 44,
                        borderRadius: '11px',
                        bgcolor: `${color}18`,
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        mb: 2,
                      }}
                    >
                      {React.cloneElement(icon, { sx: { color, fontSize: 22 } })}
                    </Box>
                    <Typography variant="h6" fontWeight={700} gutterBottom>
                      {title}
                    </Typography>
                    <Typography variant="body2" color="text.secondary" sx={{ lineHeight: 1.65 }}>
                      {description}
                    </Typography>
                  </Box>
                </Grid>
              ))}
            </Grid>
          </Container>
        </Box>

        {/* ── Footer ───────────────────────────────────────────────────────── */}
        <Box
          component="footer"
          sx={{
            py: 3,
            borderTop: '1px solid',
            borderColor: 'divider',
            textAlign: 'center',
          }}
        >
          <Container maxWidth="lg">
            <Typography variant="caption" color="text.secondary">
              © {new Date().getFullYear()} PeopleCore HR · Employee Management Portal
            </Typography>
          </Container>
        </Box>
      </Box>
    </>
  );
}
