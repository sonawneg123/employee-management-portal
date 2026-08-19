/**
 * @fileoverview WelcomeCard — premium personalised greeting banner.
 *
 * Deep navy background with gold accents, atmospheric design.
 * Greets the authenticated user by first name with a time-appropriate salutation,
 * displays today's date, and shows a role badge.
 * Premium SaaS HR product aesthetic.
 */

import React, { useMemo } from 'react';
import { Box, Chip, Typography } from '@mui/material';
import WbSunnyRoundedIcon from '@mui/icons-material/WbSunnyRounded';
import Brightness3RoundedIcon from '@mui/icons-material/Brightness3Rounded';
import NightsStayRoundedIcon from '@mui/icons-material/NightsStayRounded';
import { useAuth } from '@/hooks/useAuth';
import { ROLES } from '@/constants/roles';

/**
 * @returns {{ greeting: string, Icon: React.ElementType }}
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
      bg: 'rgba(255,255,255,0.12)',
      color: '#fff',
      border: 'rgba(255,255,255,0.25)',
    };
  if (roles.includes(ROLES.ADMIN))
    return {
      label: 'Administrator',
      bg: 'rgba(239,68,68,0.2)',
      color: '#FECACA',
      border: 'rgba(239,68,68,0.35)',
    };
  if (roles.includes(ROLES.HR))
    return {
      label: 'HR Manager',
      bg: 'rgba(245,197,24,0.18)',
      color: '#F5C518',
      border: 'rgba(245,197,24,0.35)',
    };
  if (roles.includes(ROLES.MANAGER))
    return {
      label: 'Manager',
      bg: 'rgba(16,185,129,0.18)',
      color: '#6EE7B7',
      border: 'rgba(16,185,129,0.35)',
    };
  return {
    label: 'Employee',
    bg: 'rgba(255,255,255,0.12)',
    color: 'rgba(255,255,255,0.9)',
    border: 'rgba(255,255,255,0.25)',
  };
}

/**
 * Dashboard welcome banner — deep navy with gold accents.
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
        borderRadius: '24px',
        background: 'linear-gradient(135deg, #0F1628 0%, #1A2342 50%, #2D3A6B 100%)',
        p: { xs: 3, sm: 3.5 },
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        flexWrap: 'wrap',
        gap: 2,
        position: 'relative',
        overflow: 'hidden',
        boxShadow: '0 8px 40px rgba(26,35,66,0.35)',
        animation: 'fadeUp 0.28s ease-out both',
        '@media (prefers-reduced-motion: reduce)': { animation: 'none' },
        // Atmospheric glow
        '&::before': {
          content: '""',
          position: 'absolute',
          top: -60,
          right: -60,
          width: 200,
          height: 200,
          borderRadius: '50%',
          background: 'radial-gradient(circle, rgba(245,197,24,0.12) 0%, transparent 70%)',
          pointerEvents: 'none',
        },
        '&::after': {
          content: '""',
          position: 'absolute',
          bottom: -80,
          left: 40,
          width: 260,
          height: 260,
          borderRadius: '50%',
          background: 'radial-gradient(circle, rgba(79,106,181,0.15) 0%, transparent 70%)',
          pointerEvents: 'none',
        },
      }}
    >
      {/* Left: greeting text */}
      <Box sx={{ position: 'relative', zIndex: 1 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 0.75 }}>
          <Box
            sx={{
              width: 40,
              height: 40,
              borderRadius: '12px',
              background: 'linear-gradient(135deg, rgba(245,197,24,0.25), rgba(245,197,24,0.1))',
              border: '1px solid rgba(245,197,24,0.3)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              flexShrink: 0,
            }}
            aria-hidden="true"
          >
            <Icon sx={{ color: '#F5C518', fontSize: 20 }} />
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
        <Typography variant="body2" sx={{ color: 'rgba(255,255,255,0.55)', pl: 0.5 }}>
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
          backdropFilter: 'blur(8px)',
          letterSpacing: '0.02em',
        }}
        aria-label={`Your role: ${roleMeta.label}`}
      />
    </Box>
  );
}
