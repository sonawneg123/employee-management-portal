/**
 * @fileoverview Topbar — premium fixed application bar for PeopleCore HR.
 *
 * Warm cream / white background with deep navy text and gold accents.
 * Premium SaaS style — clean and spacious.
 *
 * Features:
 * - Mobile hamburger toggle
 * - Branding visible on mobile
 * - Notification bell
 * - Dark/light mode toggle
 * - User avatar with name + role
 * - Dropdown menu (profile, settings, logout)
 */

import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  AppBar,
  Avatar,
  Box,
  Chip,
  Divider,
  IconButton,
  Menu,
  MenuItem,
  Toolbar,
  Tooltip,
  Typography,
  useMediaQuery,
  useTheme,
} from '@mui/material';
import MenuRoundedIcon from '@mui/icons-material/MenuRounded';
import LightModeRoundedIcon from '@mui/icons-material/LightModeRounded';
import DarkModeRoundedIcon from '@mui/icons-material/DarkModeRounded';
import LogoutRoundedIcon from '@mui/icons-material/LogoutRounded';
import PersonRoundedIcon from '@mui/icons-material/PersonRounded';
import KeyboardArrowDownRoundedIcon from '@mui/icons-material/KeyboardArrowDownRounded';
import SettingsRoundedIcon from '@mui/icons-material/SettingsRounded';
import PeopleAltIcon from '@mui/icons-material/PeopleAlt';

import { useAuth } from '@/contexts/AuthContext';
import { useThemeMode } from '@/theme/ThemeContext';
import { ROUTES } from '@/constants/routes';
import { ROLES } from '@/constants/roles';
import NotificationBell from '@/components/notifications/NotificationBell';

/**
 * Maps roles to a short human-readable label.
 *
 * @param {string[]} roles
 * @returns {string}
 */
function getRoleChipLabel(roles) {
  if (!roles?.length) return 'User';
  if (roles.includes(ROLES.ADMIN)) return 'Admin';
  if (roles.includes(ROLES.HR)) return 'HR Manager';
  if (roles.includes(ROLES.MANAGER)) return 'Manager';
  return 'Employee';
}

/** Role colour tokens for the chip in the menu. */
function getRoleChipColor(roles) {
  if (!roles?.length)
    return { bg: 'rgba(107,114,128,0.1)', color: '#6B7280', border: 'rgba(107,114,128,0.2)' };
  if (roles.includes(ROLES.ADMIN))
    return { bg: 'rgba(239,68,68,0.08)', color: '#DC2626', border: 'rgba(239,68,68,0.2)' };
  if (roles.includes(ROLES.HR))
    return {
      bg: 'rgba(26,35,66,0.08)',
      color: '#1A2342',
      border: 'rgba(26,35,66,0.2)',
    };
  if (roles.includes(ROLES.MANAGER))
    return { bg: 'rgba(16,185,129,0.08)', color: '#059669', border: 'rgba(16,185,129,0.2)' };
  return { bg: 'rgba(245,197,24,0.12)', color: '#92700A', border: 'rgba(245,197,24,0.3)' };
}

/**
 * Modern top application bar — premium SaaS style.
 *
 * @param {{
 *   onMenuClick:      () => void,
 *   onCollapseToggle: () => void,
 *   sidebarWidth:     number,
 *   collapsed:        boolean,
 * }} props
 * @returns {JSX.Element}
 */
export default function Topbar({ onMenuClick, sidebarWidth }) {
  const { user, logout } = useAuth();
  const { mode, toggleMode } = useThemeMode();
  const profilePhotoUrl = user?.profilePhotoUrl ?? null;
  const navigate = useNavigate();
  const theme = useTheme();
  const isMobileScreen = useMediaQuery(theme.breakpoints.down('md'));
  const isDark = mode === 'dark';

  const [anchorEl, setAnchorEl] = useState(null);
  const menuOpen = Boolean(anchorEl);

  const handleAvatarClick = (e) => setAnchorEl(e.currentTarget);
  const handleMenuClose = () => setAnchorEl(null);

  const handleProfileClick = () => {
    handleMenuClose();
    navigate(ROUTES.PROFILE);
  };
  const handleSettingsClick = () => {
    handleMenuClose();
    navigate(ROUTES.SETTINGS);
  };
  const handleLogout = () => {
    handleMenuClose();
    logout();
  };

  const initials = user
    ? `${user.firstName?.[0] ?? ''}${user.lastName?.[0] ?? ''}`.toUpperCase()
    : '?';

  const roleLabel = getRoleChipLabel(user?.roles);
  const roleChipColor = getRoleChipColor(user?.roles);

  // Premium SaaS topbar: warm white / cream background
  const topbarBg = isDark ? '#0C1220' : '#FFFFFF';
  const topbarBorder = isDark ? 'rgba(240,237,230,0.07)' : '#EBE6DA';
  const iconBtnSx = {
    color: isDark ? 'rgba(240,237,230,0.6)' : '#6B7280',
    bgcolor: isDark ? 'rgba(255,255,255,0.05)' : 'rgba(26,35,66,0.04)',
    borderRadius: '10px',
    width: 36,
    height: 36,
    '&:hover': {
      bgcolor: isDark ? 'rgba(255,255,255,0.09)' : 'rgba(26,35,66,0.08)',
      color: isDark ? '#F0EDE6' : '#1A2342',
      transform: 'scale(1.05)',
    },
    '&:active': {
      transform: 'scale(0.95)',
    },
    transition: 'all 0.15s ease',
    '@media (prefers-reduced-motion: reduce)': {
      transition: 'none',
      '&:hover': { transform: 'none' },
      '&:active': { transform: 'none' },
    },
  };

  return (
    <AppBar
      position="fixed"
      elevation={0}
      sx={{
        width: { md: `calc(100% - ${sidebarWidth}px)` },
        ml: { md: `${sidebarWidth}px` },
        bgcolor: topbarBg,
        borderBottom: `1px solid ${topbarBorder}`,
        color: 'text.primary',
        backdropFilter: 'blur(12px)',
        transition:
          'margin-left 0.28s cubic-bezier(0.4,0,0.2,1), width 0.28s cubic-bezier(0.4,0,0.2,1)',
        boxShadow: isDark ? '0 1px 0 rgba(240,237,230,0.07)' : '0 1px 0 rgba(235,230,218,0.8)',
      }}
    >
      <Toolbar
        sx={{
          gap: 1,
          minHeight: { xs: 60, sm: 64 },
          px: { xs: 2, sm: 3 },
        }}
      >
        {/* Mobile menu toggle */}
        <IconButton
          onClick={onMenuClick}
          size="small"
          sx={{
            display: { md: 'none' },
            mr: 0.5,
            ...iconBtnSx,
          }}
          aria-label="Toggle navigation"
        >
          <MenuRoundedIcon sx={{ fontSize: 20 }} />
        </IconButton>

        {/* Mobile branding */}
        {isMobileScreen && (
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mr: 'auto' }}>
            <Box
              sx={{
                width: 28,
                height: 28,
                borderRadius: '8px',
                background: 'linear-gradient(135deg, #F5C518, #C49A00)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
              aria-hidden="true"
            >
              <PeopleAltIcon sx={{ color: '#1A2342', fontSize: 15 }} />
            </Box>
            <Typography
              variant="body2"
              fontWeight={800}
              sx={{ color: isDark ? '#F0EDE6' : '#1A2342', letterSpacing: '-0.02em' }}
            >
              PeopleCore HR
            </Typography>
          </Box>
        )}

        {/* Flexible spacer on desktop */}
        {!isMobileScreen && <Box sx={{ flexGrow: 1 }} />}

        {/* Right-side controls */}
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.75 }}>
          {/* Notification bell */}
          <NotificationBell />

          {/* Theme toggle */}
          <Tooltip title={mode === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'}>
            <IconButton
              onClick={toggleMode}
              size="small"
              sx={iconBtnSx}
              aria-label="Toggle colour mode"
            >
              {mode === 'dark' ? (
                <LightModeRoundedIcon sx={{ fontSize: 18 }} />
              ) : (
                <DarkModeRoundedIcon sx={{ fontSize: 18 }} />
              )}
            </IconButton>
          </Tooltip>

          {/* User avatar + dropdown trigger */}
          <Box
            onClick={handleAvatarClick}
            sx={{
              display: 'flex',
              alignItems: 'center',
              gap: 1,
              ml: 0.25,
              cursor: 'pointer',
              borderRadius: '12px',
              px: 1.25,
              py: 0.75,
              border: '1.5px solid',
              borderColor: menuOpen
                ? isDark
                  ? '#4F6AB5'
                  : '#1A2342'
                : isDark
                  ? 'rgba(240,237,230,0.1)'
                  : '#E8E3D8',
              bgcolor: isDark
                ? menuOpen
                  ? 'rgba(26,35,66,0.4)'
                  : 'rgba(255,255,255,0.04)'
                : menuOpen
                  ? 'rgba(26,35,66,0.04)'
                  : '#FAF7F0',
              transition: 'all 0.15s ease',
              '&:hover': {
                borderColor: isDark ? '#4F6AB5' : '#1A2342',
                bgcolor: isDark ? 'rgba(26,35,66,0.3)' : 'rgba(26,35,66,0.04)',
              },
            }}
            role="button"
            aria-label="Open account menu"
            aria-haspopup="true"
            aria-expanded={menuOpen}
          >
            <Avatar
              src={profilePhotoUrl || undefined}
              sx={{
                width: 30,
                height: 30,
                background: 'linear-gradient(135deg, #1A2342, #2D3A6B)',
                fontSize: '0.7rem',
                fontWeight: 700,
                flexShrink: 0,
                border: '1.5px solid rgba(245,197,24,0.35)',
              }}
            >
              {!profilePhotoUrl && initials}
            </Avatar>
            {!isMobileScreen && (
              <Box sx={{ minWidth: 0 }}>
                <Typography
                  variant="caption"
                  fontWeight={700}
                  noWrap
                  sx={{
                    display: 'block',
                    lineHeight: 1.2,
                    color: isDark ? '#F0EDE6' : '#1A2342',
                    fontSize: '0.8rem',
                  }}
                >
                  {user?.firstName} {user?.lastName}
                </Typography>
                <Typography
                  variant="caption"
                  noWrap
                  sx={{ fontSize: '0.65rem', color: 'text.secondary', display: 'block' }}
                >
                  {roleLabel}
                </Typography>
              </Box>
            )}
            <KeyboardArrowDownRoundedIcon
              sx={{
                fontSize: 16,
                color: 'text.secondary',
                transform: menuOpen ? 'rotate(180deg)' : 'none',
                transition: 'transform 0.2s ease',
                display: { xs: 'none', sm: 'block' },
                flexShrink: 0,
              }}
            />
          </Box>
        </Box>

        {/* Dropdown menu */}
        <Menu
          anchorEl={anchorEl}
          open={menuOpen}
          onClose={handleMenuClose}
          transformOrigin={{ horizontal: 'right', vertical: 'top' }}
          anchorOrigin={{ horizontal: 'right', vertical: 'bottom' }}
          PaperProps={{
            elevation: 2,
            sx: {
              minWidth: 240,
              mt: 1.5,
              overflow: 'visible',
              bgcolor: isDark ? '#131C2E' : '#FFFFFF',
            },
          }}
        >
          {user && (
            <Box sx={{ px: 2, py: 1.5 }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 0.75 }}>
                <Avatar
                  src={profilePhotoUrl || undefined}
                  sx={{
                    width: 42,
                    height: 42,
                    background: 'linear-gradient(135deg, #1A2342, #2D3A6B)',
                    fontSize: '0.9rem',
                    fontWeight: 700,
                    flexShrink: 0,
                    border: '2px solid rgba(245,197,24,0.3)',
                  }}
                >
                  {!profilePhotoUrl && initials}
                </Avatar>
                <Box sx={{ minWidth: 0 }}>
                  <Typography
                    variant="body2"
                    fontWeight={700}
                    noWrap
                    sx={{ color: isDark ? '#F0EDE6' : '#1A2342' }}
                  >
                    {user.firstName} {user.lastName}
                  </Typography>
                  <Typography
                    variant="caption"
                    color="text.secondary"
                    noWrap
                    sx={{ display: 'block' }}
                  >
                    {user.email}
                  </Typography>
                </Box>
              </Box>
              <Chip
                label={roleLabel}
                size="small"
                sx={{
                  mt: 0.25,
                  height: 22,
                  fontSize: '0.675rem',
                  fontWeight: 700,
                  bgcolor: roleChipColor.bg,
                  color: roleChipColor.color,
                  border: `1px solid ${roleChipColor.border}`,
                }}
              />
            </Box>
          )}

          <Divider />

          <MenuItem onClick={handleProfileClick} sx={{ gap: 1.5, mt: 0.5 }}>
            <PersonRoundedIcon fontSize="small" sx={{ color: 'text.secondary' }} />
            <Typography variant="body2" fontWeight={500}>
              My Profile
            </Typography>
          </MenuItem>

          <MenuItem onClick={handleSettingsClick} sx={{ gap: 1.5 }}>
            <SettingsRoundedIcon fontSize="small" sx={{ color: 'text.secondary' }} />
            <Typography variant="body2" fontWeight={500}>
              Settings
            </Typography>
          </MenuItem>

          <Divider sx={{ my: 0.5 }} />

          <MenuItem
            onClick={handleLogout}
            sx={{
              gap: 1.5,
              color: 'error.main',
              mb: 0.5,
              '&:hover': { bgcolor: 'rgba(239,68,68,0.06)' },
            }}
          >
            <LogoutRoundedIcon fontSize="small" />
            <Typography variant="body2" fontWeight={600}>
              Sign out
            </Typography>
          </MenuItem>
        </Menu>
      </Toolbar>
    </AppBar>
  );
}
