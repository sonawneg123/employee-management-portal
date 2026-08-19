/**
 * @fileoverview LandingPage — premium enterprise HR SaaS landing page.
 *
 * Hero section with product preview mockup, feature grid, role-based auth
 * option cards, and a clean footer. Fully responsive.
 */

import React from 'react';
import { Link as RouterLink } from 'react-router-dom';
import { Helmet } from 'react-helmet-async';
import { Box, Button, Chip, Container, Divider, Grid, Typography, useTheme } from '@mui/material';
import PeopleAltIcon from '@mui/icons-material/PeopleAlt';
import AdminPanelSettingsRoundedIcon from '@mui/icons-material/AdminPanelSettingsRounded';
import WorkRoundedIcon from '@mui/icons-material/WorkRounded';
import PersonRoundedIcon from '@mui/icons-material/PersonRounded';
import PersonAddRoundedIcon from '@mui/icons-material/PersonAddRounded';
import EventAvailableRoundedIcon from '@mui/icons-material/EventAvailableRounded';
import BarChartRoundedIcon from '@mui/icons-material/BarChartRounded';
import AccessTimeRoundedIcon from '@mui/icons-material/AccessTimeRounded';
import AssessmentRoundedIcon from '@mui/icons-material/AssessmentRounded';
import ShieldRoundedIcon from '@mui/icons-material/ShieldRounded';
import ArrowForwardRoundedIcon from '@mui/icons-material/ArrowForwardRounded';
import { ROUTES } from '@/constants/routes';

// ── Feature data ──────────────────────────────────────────────────────────────

const FEATURES = [
  {
    icon: <PeopleAltIcon />,
    title: 'Employee Directory',
    description: 'Centralised records for every team member — from hire to retire.',
    color: '#4F46E5',
    bg: 'rgba(79,70,229,0.1)',
  },
  {
    icon: <EventAvailableRoundedIcon />,
    title: 'Leave Management',
    description: 'Submit, approve, and track leave requests in real time.',
    color: '#7C3AED',
    bg: 'rgba(124,58,237,0.1)',
  },
  {
    icon: <AccessTimeRoundedIcon />,
    title: 'Attendance Tracking',
    description: 'Monitor attendance patterns and generate daily presence reports.',
    color: '#10B981',
    bg: 'rgba(16,185,129,0.1)',
  },
  {
    icon: <BarChartRoundedIcon />,
    title: 'Department Analytics',
    description: 'Visualise headcount distribution and workforce metrics.',
    color: '#F59E0B',
    bg: 'rgba(245,158,11,0.1)',
  },
  {
    icon: <AssessmentRoundedIcon />,
    title: 'Performance Reviews',
    description: 'Structured evaluation cycles with rating history.',
    color: '#3B82F6',
    bg: 'rgba(59,130,246,0.1)',
  },
  {
    icon: <ShieldRoundedIcon />,
    title: 'Role-Based Access',
    description: 'Granular permissions for Admin, HR, Manager, and Employee.',
    color: '#EF4444',
    bg: 'rgba(239,68,68,0.1)',
  },
];

// ── Auth option cards ─────────────────────────────────────────────────────────

const LOGIN_OPTIONS = [
  {
    icon: <AdminPanelSettingsRoundedIcon />,
    label: 'Sign in as Admin',
    description: 'System administration & user management',
    to: ROUTES.LOGIN_ADMIN,
    color: '#EF4444',
    bg: 'rgba(239,68,68,0.08)',
  },
  {
    icon: <WorkRoundedIcon />,
    label: 'Sign in as HR',
    description: 'HR operations, employees & leave approvals',
    to: ROUTES.LOGIN_HR,
    color: '#4F46E5',
    bg: 'rgba(79,70,229,0.08)',
  },
  {
    icon: <PersonRoundedIcon />,
    label: 'Sign in as Employee',
    description: 'Self-service portal — leaves, attendance & reviews',
    to: ROUTES.LOGIN_EMPLOYEE,
    color: '#10B981',
    bg: 'rgba(16,185,129,0.08)',
  },
];

const REGISTER_OPTIONS = [
  {
    icon: <WorkRoundedIcon />,
    label: 'Create HR Account',
    description: 'Register a new HR portal account',
    to: ROUTES.REGISTER_HR,
    color: '#4F46E5',
    bg: 'rgba(79,70,229,0.08)',
    primary: true,
  },
  {
    icon: <PersonAddRoundedIcon />,
    label: 'Create Employee Account',
    description: 'Register as a new employee',
    to: ROUTES.REGISTER_EMPLOYEE,
    color: '#10B981',
    bg: 'rgba(16,185,129,0.08)',
    primary: false,
  },
];

// ── Sub-components ────────────────────────────────────────────────────────────

/**
 * Auth option clickable card.
 */
function AuthOptionCard({ icon, label, description, to, color, bg, primary }) {
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
        borderColor: primary ? color : isDark ? 'rgba(241,245,249,0.1)' : 'divider',
        bgcolor: primary ? (isDark ? `${color}22` : bg) : 'background.paper',
        textDecoration: 'none',
        transition: 'all 0.2s ease',
        '&:hover': {
          boxShadow: isDark ? '0 6px 24px rgba(0,0,0,0.4)' : `0 6px 24px ${color}22`,
          transform: 'translateY(-2px)',
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
      <Box sx={{ minWidth: 0, flex: 1 }}>
        <Typography variant="body2" fontWeight={700} color="text.primary" noWrap>
          {label}
        </Typography>
        <Typography variant="caption" color="text.secondary" display="block">
          {description}
        </Typography>
      </Box>
      <ArrowForwardRoundedIcon sx={{ fontSize: 16, color: 'text.disabled', flexShrink: 0 }} />
    </Box>
  );
}

/**
 * Feature card used in the feature grid.
 */
function FeatureCard({ icon, title, description, color, bg }) {
  return (
    <Box
      sx={{
        p: 2.5,
        borderRadius: '14px',
        border: '1px solid',
        borderColor: 'divider',
        bgcolor: 'background.paper',
        height: '100%',
        transition: 'box-shadow 0.2s ease, transform 0.2s ease',
        '&:hover': {
          boxShadow: (theme) =>
            theme.palette.mode === 'dark'
              ? '0 4px 20px rgba(0,0,0,0.4)'
              : '0 4px 20px rgba(0,0,0,0.08)',
          transform: 'translateY(-2px)',
        },
      }}
    >
      <Box
        sx={{
          width: 40,
          height: 40,
          borderRadius: '10px',
          bgcolor: bg,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          mb: 1.5,
        }}
      >
        {React.cloneElement(icon, { sx: { color, fontSize: 20 } })}
      </Box>
      <Typography variant="subtitle2" fontWeight={700} sx={{ mb: 0.5 }}>
        {title}
      </Typography>
      <Typography variant="caption" color="text.secondary" sx={{ lineHeight: 1.6 }}>
        {description}
      </Typography>
    </Box>
  );
}

/**
 * Inline dashboard preview mockup — pure CSS/SVG, no images needed.
 */
function DashboardPreview() {
  const theme = useTheme();
  const isDark = theme.palette.mode === 'dark';

  const bg = isDark ? '#111827' : '#F8FAFC';
  const card = isDark ? '#1E293B' : '#FFFFFF';
  const border = isDark ? 'rgba(241,245,249,0.08)' : '#E2E8F0';
  const text1 = isDark ? '#F1F5F9' : '#0F172A';
  const text2 = isDark ? '#64748B' : '#94A3B8';
  const sidebar = isDark ? '#0F172A' : '#0F172A';

  return (
    <Box
      aria-hidden="true"
      sx={{
        width: '100%',
        maxWidth: 580,
        mx: 'auto',
        borderRadius: '20px',
        overflow: 'hidden',
        border: '1px solid',
        borderColor: 'divider',
        boxShadow: isDark ? '0 24px 64px rgba(0,0,0,0.6)' : '0 24px 64px rgba(0,0,0,0.12)',
        animation: 'fadeUp 0.6s ease 0.2s both',
      }}
    >
      <svg
        viewBox="0 0 580 380"
        xmlns="http://www.w3.org/2000/svg"
        style={{ display: 'block', width: '100%' }}
      >
        {/* Background */}
        <rect width="580" height="380" fill={bg} />

        {/* Sidebar */}
        <rect width="56" height="380" fill={sidebar} />
        <circle cx="28" cy="32" r="10" fill="#4F46E5" />
        {[80, 110, 140, 170, 200, 230].map((y, i) => (
          <rect
            key={i}
            x="16"
            y={y - 6}
            width="24"
            height="12"
            rx="3"
            fill={i === 0 ? '#4F46E5' : 'rgba(255,255,255,0.15)'}
          />
        ))}

        {/* Topbar */}
        <rect x="56" y="0" width="524" height="48" fill={card} />
        <rect x="56" y="47" width="524" height="1" fill={border} />
        <rect
          x="420"
          y="14"
          width="60"
          height="20"
          rx="4"
          fill={isDark ? 'rgba(79,70,229,0.2)' : 'rgba(79,70,229,0.1)'}
        />
        <circle cx="510" cy="24" r="12" fill="#4F46E5" />
        <rect x="490" y="17" width="30" height="7" rx="2" fill="rgba(255,255,255,0)" />

        {/* Welcome banner */}
        <rect
          x="68"
          y="60"
          width="500"
          height="56"
          rx="10"
          fill={isDark ? 'rgba(79,70,229,0.15)' : 'rgba(79,70,229,0.06)'}
        />
        <rect
          x="68"
          y="60"
          width="500"
          height="56"
          rx="10"
          stroke="#4F46E5"
          strokeWidth="0.5"
          fill="none"
          strokeOpacity="0.3"
        />
        <rect x="84" y="75" width="140" height="12" rx="4" fill={text1} fillOpacity="0.7" />
        <rect x="84" y="92" width="90" height="8" rx="3" fill={text2} fillOpacity="0.6" />

        {/* KPI Cards row */}
        {[
          { x: 68, color: '#4F46E5', bg: 'rgba(79,70,229,0.1)' },
          { x: 199, color: '#7C3AED', bg: 'rgba(124,58,237,0.1)' },
          { x: 330, color: '#F59E0B', bg: 'rgba(245,158,11,0.1)' },
          { x: 461, color: '#10B981', bg: 'rgba(16,185,129,0.1)' },
        ].map(({ x, bg: cbg }, i) => (
          <g key={i}>
            <rect
              x={x}
              y="128"
              width="119"
              height="80"
              rx="10"
              fill={card}
              stroke={border}
              strokeWidth="1"
            />
            <rect x={x + 8} y="137" width="24" height="24" rx="6" fill={cbg} />
            <rect x={x + 8} y="169" width="50" height="14" rx="4" fill={text1} fillOpacity="0.7" />
            <rect x={x + 8} y="189" width="70" height="8" rx="3" fill={text2} fillOpacity="0.5" />
          </g>
        ))}

        {/* Chart cards */}
        <rect
          x="68"
          y="220"
          width="240"
          height="140"
          rx="10"
          fill={card}
          stroke={border}
          strokeWidth="1"
        />
        <rect x="80" y="232" width="80" height="10" rx="3" fill={text1} fillOpacity="0.6" />
        {/* Bar chart */}
        {[
          { x: 85, h: 50, color: '#4F46E5' },
          { x: 110, h: 70, color: '#7C3AED' },
          { x: 135, h: 40, color: '#10B981' },
          { x: 160, h: 85, color: '#4F46E5' },
          { x: 185, h: 55, color: '#7C3AED' },
          { x: 210, h: 65, color: '#10B981' },
          { x: 235, h: 45, color: '#4F46E5' },
          { x: 260, h: 75, color: '#7C3AED' },
        ].map(({ x, h, color }, i) => (
          <rect key={i} x={x} y={345 - h} width="18" height={h} rx="3" fill={color} opacity="0.8" />
        ))}

        <rect
          x="320"
          y="220"
          width="248"
          height="140"
          rx="10"
          fill={card}
          stroke={border}
          strokeWidth="1"
        />
        <rect x="332" y="232" width="80" height="10" rx="3" fill={text1} fillOpacity="0.6" />
        {/* Donut chart */}
        <circle
          cx="412"
          cy="305"
          r="45"
          fill="none"
          stroke="#4F46E5"
          strokeWidth="20"
          strokeDasharray="140 283"
        />
        <circle
          cx="412"
          cy="305"
          r="45"
          fill="none"
          stroke="#7C3AED"
          strokeWidth="20"
          strokeDasharray="90 283"
          strokeDashoffset="-140"
        />
        <circle
          cx="412"
          cy="305"
          r="45"
          fill="none"
          stroke="#10B981"
          strokeWidth="20"
          strokeDasharray="53 283"
          strokeDashoffset="-230"
        />
        <circle
          cx="412"
          cy="305"
          r="45"
          fill="none"
          stroke="#F59E0B"
          strokeWidth="20"
          strokeDasharray="70"
          strokeDashoffset="-283"
        />
        {/* Legend */}
        <rect x="465" y="275" width="8" height="8" rx="2" fill="#4F46E5" />
        <rect x="465" y="290" width="8" height="8" rx="2" fill="#7C3AED" />
        <rect x="465" y="305" width="8" height="8" rx="2" fill="#10B981" />
        <rect x="465" y="320" width="8" height="8" rx="2" fill="#F59E0B" />
      </svg>
    </Box>
  );
}

// ── Page ──────────────────────────────────────────────────────────────────────

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
        {/* ── Top navigation ───────────────────────────────────────────────── */}
        <Box
          component="nav"
          sx={{
            borderBottom: '1px solid',
            borderColor: 'divider',
            bgcolor: isDark ? 'rgba(17,24,39,0.9)' : 'rgba(255,255,255,0.9)',
            backdropFilter: 'blur(8px)',
            position: 'sticky',
            top: 0,
            zIndex: 100,
          }}
        >
          <Container maxWidth="lg">
            <Box
              sx={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                py: 1.75,
              }}
            >
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <Box
                  sx={{
                    width: 30,
                    height: 30,
                    borderRadius: '8px',
                    background: 'linear-gradient(135deg, #F5C518, #C49A00)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                  }}
                >
                  <PeopleAltIcon sx={{ color: '#1A2342', fontSize: 17 }} />
                </Box>
                <Typography
                  variant="body1"
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

              <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
                <Button
                  component={RouterLink}
                  to={ROUTES.LOGIN}
                  variant="text"
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
                  to={ROUTES.REGISTER}
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

        {/* ── Hero ─────────────────────────────────────────────────────────── */}
        <Box sx={{ pt: { xs: 6, md: 9 }, pb: { xs: 4, md: 6 } }}>
          <Container maxWidth="lg">
            <Grid container spacing={{ xs: 4, md: 6 }} alignItems="center">
              <Grid size={{ xs: 12, md: 5 }}>
                <Box sx={{ animation: 'fadeUp 0.5s ease' }}>
                  <Chip
                    label="Employee Management Portal"
                    size="small"
                    sx={{
                      mb: 2.5,
                      bgcolor: 'rgba(245,197,24,0.12)',
                      color: '#92700A',
                      fontWeight: 700,
                      border: '1px solid rgba(245,197,24,0.3)',
                    }}
                  />
                  <Typography
                    variant="h1"
                    fontWeight={800}
                    sx={{
                      mb: 2,
                      letterSpacing: '-0.03em',
                      lineHeight: 1.15,
                      fontSize: { xs: '2rem', md: '2.5rem' },
                    }}
                  >
                    Modern HR platform for your team
                  </Typography>
                  <Typography
                    variant="body1"
                    color="text.secondary"
                    sx={{ mb: 3.5, lineHeight: 1.7, maxWidth: 400 }}
                  >
                    Manage employees, approve leaves, track attendance, and review performance — all
                    in one place.
                  </Typography>
                  <Box sx={{ display: 'flex', gap: 1.5, flexWrap: 'wrap' }}>
                    <Button
                      component={RouterLink}
                      to={ROUTES.LOGIN}
                      variant="contained"
                      size="large"
                      sx={{ fontWeight: 700 }}
                    >
                      Sign In
                    </Button>
                    <Button
                      component={RouterLink}
                      to={ROUTES.REGISTER}
                      variant="outlined"
                      size="large"
                      sx={{ fontWeight: 600 }}
                    >
                      Create Account
                    </Button>
                  </Box>
                </Box>
              </Grid>

              {/* Dashboard preview */}
              <Grid size={{ xs: 12, md: 7 }}>
                <DashboardPreview />
              </Grid>
            </Grid>
          </Container>
        </Box>

        {/* ── Features grid ────────────────────────────────────────────────── */}
        <Box
          sx={{
            py: { xs: 5, md: 7 },
            bgcolor: isDark ? 'rgba(255,255,255,0.02)' : 'rgba(0,0,0,0.015)',
          }}
        >
          <Container maxWidth="lg">
            <Box sx={{ textAlign: 'center', mb: 5, animation: 'fadeUp 0.5s ease' }}>
              <Typography
                variant="overline"
                color="primary.main"
                fontWeight={700}
                sx={{ mb: 1, display: 'block' }}
              >
                Core Features
              </Typography>
              <Typography variant="h3" fontWeight={800} sx={{ letterSpacing: '-0.02em' }}>
                Everything you need to manage your workforce
              </Typography>
            </Box>
            <Grid container spacing={2}>
              {FEATURES.map((feat) => (
                <Grid key={feat.title} size={{ xs: 12, sm: 6, md: 4 }}>
                  <FeatureCard {...feat} />
                </Grid>
              ))}
            </Grid>
          </Container>
        </Box>

        {/* ── Auth options ─────────────────────────────────────────────────── */}
        <Box sx={{ py: { xs: 5, md: 7 } }}>
          <Container maxWidth="md">
            <Grid container spacing={4}>
              {/* Sign In */}
              <Grid size={{ xs: 12, md: 6 }}>
                <Box sx={{ mb: 2 }}>
                  <Typography variant="h5" fontWeight={800} sx={{ mb: 0.5 }}>
                    Sign In
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    Choose your role to access the portal
                  </Typography>
                </Box>
                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.25 }}>
                  {LOGIN_OPTIONS.map((opt) => (
                    <AuthOptionCard key={opt.label} {...opt} />
                  ))}
                </Box>
              </Grid>

              <Grid size={{ xs: 12, md: 6 }}>
                <Box sx={{ mb: 2 }}>
                  <Typography variant="h5" fontWeight={800} sx={{ mb: 0.5 }}>
                    Create Account
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    New to PeopleCore? Register here
                  </Typography>
                </Box>
                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.25 }}>
                  {REGISTER_OPTIONS.map((opt) => (
                    <AuthOptionCard key={opt.label} {...opt} />
                  ))}
                </Box>
                <Box
                  sx={{
                    mt: 2,
                    p: 2,
                    borderRadius: '12px',
                    bgcolor: isDark ? 'rgba(239,68,68,0.08)' : 'rgba(239,68,68,0.05)',
                    border: '1px solid rgba(239,68,68,0.15)',
                  }}
                >
                  <Typography variant="caption" color="text.secondary" sx={{ lineHeight: 1.6 }}>
                    <strong style={{ color: '#EF4444' }}>Admin accounts</strong> are not available
                    for public registration. Contact your system administrator.
                  </Typography>
                </Box>
              </Grid>
            </Grid>
          </Container>
        </Box>

        {/* ── Footer ───────────────────────────────────────────────────────── */}
        <Divider />
        <Box
          component="footer"
          sx={{
            py: 3,
            mt: 'auto',
            bgcolor: isDark ? 'rgba(255,255,255,0.02)' : 'rgba(0,0,0,0.02)',
          }}
        >
          <Container maxWidth="lg">
            <Box
              sx={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                flexWrap: 'wrap',
                gap: 2,
              }}
            >
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <Box
                  sx={{
                    width: 24,
                    height: 24,
                    borderRadius: '6px',
                    background: 'linear-gradient(135deg, #4F46E5, #7C3AED)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                  }}
                >
                  <PeopleAltIcon sx={{ color: '#fff', fontSize: 13 }} />
                </Box>
                <Typography variant="caption" fontWeight={700} color="text.secondary">
                  PeopleCore HR
                </Typography>
              </Box>
              <Typography variant="caption" color="text.disabled">
                © {new Date().getFullYear()} Employee Management Portal. All rights reserved.
              </Typography>
            </Box>
          </Container>
        </Box>
      </Box>
    </>
  );
}
