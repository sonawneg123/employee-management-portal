/**
 * @fileoverview WelcomeCard — personalised greeting banner.
 *
 * Greets the authenticated user by first name with a time-appropriate salutation,
 * displays today's date, and shows a role badge.
 * Uses a rich indigo/navy gradient banner design.
 */

import React, { useMemo } from 'react';
import { Box, Chip, Typography } from '@mui/material';
import WbSunnyRoundedIcon from '@mui/icons-material/WbSunnyRounded';
import Brightness3RoundedIcon from '@mui/icons-material/Brightness3Rounded';
import NightsStayRoundedIcon from '@mui/icons-material/NightsStayRounded';
import { useAuth } from '@/hooks/useAuth';
import { ROLES } from '@/constants/roles';

/**
 * @returns {{ greeting: string, icon: React.ElementType }}
 */
function getGreetingInfo() {
  const h = new Date().getHours();
  if (h < 12) return { greeting: 'Good morning', Icon: WbSunnyRoundedIcon };
  if (h < 17) return { greeting: 'Good afternoon', Icon: Brightness3RoundedIcon };
  return { greeting: 'Good evening', Icon: NightsStayRoundedIcon };
}

/**
 * @param {string[]} roles
 * @returns {{ label: string, bg: string, color: string, border: string }}
 */
function getRoleMeta(roles) {
  if (!roles?.length)
    return {
      label: 'User',
      bg: 'rgba(255,255,255,0.15)',
      color: '#fff',
      border: 'rgba(255,255,255,0.3)',
    };
  if (roles.includes(ROLES.ADMIN))
    return {
      label: 'Administrator',
      bg: 'rgba(239,68,68,0.2)',
      color: '#FECACA',
      border: 'rgba(239,68,68,0.4)',
    };
  if (roles.includes(ROLES.HR))
    return {
      label: 'HR Manager',
      bg: 'rgba(255,255,255,0.18)',
      color: '#fff',
      border: 'rgba(255,255,255,0.35)',
    };
  if (roles.includes(ROLES.MANAGER))
    return {
      label: 'Manager',
      bg: 'rgba(16,185,129,0.2)',
      color: '#A7F3D0',
      border: 'rgba(16,185,129,0.4)',
    };
  return {
    label: 'Employee',
    bg: 'rgba(245,158,11,0.2)',
    color: '#FDE68A',
    border: 'rgba(245,158,11,0.4)',
  };
}

/**
 * Dashboard welcome banner with gradient background.
 *
 * @returns {JSX.Element}
 */
export default function WelcomeCard() {
  const { user } = useAuth();
  const { greeting, Icon } = useMemo(getGreetingInfo, []);
  const roleMeta = useMemo(() => getRoleMeta(user?.roles), [user]);

  const today = new Date().toLocaleDateString('en-US', {
    weekday: 'long',
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });

  return (
    <Box
      sx={{
        mb: 3,
        borderRadius: '20px',
        background: (theme) =>
          theme.palette.mode === 'dark'
            ? 'linear-gradient(135deg, #1E1B4B 0%, #312E81 50%, #4338CA 100%)'
            : 'linear-gradient(135deg, #243B7A 0%, #4F46E5 60%, #7C3AED 100%)',
        p: { xs: 3, sm: 3.5 },
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        flexWrap: 'wrap',
        gap: 2,
        position: 'relative',
        overflow: 'hidden',
        boxShadow: (theme) =>
          theme.palette.mode === 'dark'
            ? '0 8px 32px rgba(79,70,229,0.3)'
            : '0 8px 32px rgba(36,59,122,0.25)',
        // Decorative circles
        '&::before': {
          content: '""',
          position: 'absolute',
          top: -40,
          right: -40,
          width: 160,
          height: 160,
          borderRadius: '50%',
          background: 'rgba(255,255,255,0.06)',
          pointerEvents: 'none',
        },
        '&::after': {
          content: '""',
          position: 'absolute',
          bottom: -60,
          right: 80,
          width: 220,
          height: 220,
          borderRadius: '50%',
          background: 'rgba(255,255,255,0.04)',
          pointerEvents: 'none',
        },
      }}
    >
      {/* Left: greeting text */}
      <Box sx={{ position: 'relative', zIndex: 1 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 0.75 }}>
          <Box
            sx={{
              width: 36,
              height: 36,
              borderRadius: '10px',
              bgcolor: 'rgba(255,255,255,0.15)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              flexShrink: 0,
            }}
            aria-hidden="true"
          >
            <Icon sx={{ color: '#FCD34D', fontSize: 20 }} />
          </Box>
          <Typography
            variant="h4"
            fontWeight={800}
            sx={{
              color: '#FFFFFF',
              letterSpacing: '-0.02em',
              lineHeight: 1.2,
              fontSize: { xs: '1.25rem', sm: '1.5rem' },
            }}
          >
            {greeting}, {user?.firstName ?? 'there'}!
          </Typography>
        </Box>
        <Typography variant="body2" sx={{ color: 'rgba(255,255,255,0.7)', pl: 0.25 }}>
          {today}
        </Typography>
      </Box>

      {/* Right: role chip */}
      <Chip
        label={roleMeta.label}
        sx={{
          height: 32,
          fontWeight: 700,
          fontSize: '0.8rem',
          bgcolor: roleMeta.bg,
          color: roleMeta.color,
          border: `1px solid ${roleMeta.border}`,
          position: 'relative',
          zIndex: 1,
          backdropFilter: 'blur(4px)',
        }}
        aria-label={`Your role: ${roleMeta.label}`}
      />
    </Box>
  );
}
