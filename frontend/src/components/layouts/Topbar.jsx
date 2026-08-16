/**
 * @fileoverview Topbar — premium fixed application bar.
 *
 * Features:
 * - Mobile hamburger toggle
 * - Page-level breadcrumb/title area
 * - Dark/light mode toggle
 * - User avatar with name + role chip
 * - Dropdown menu (profile, logout)
 */

import React, { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
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
  if (roles.includes(ROLES.HR)) return 'HR';
  if (roles.includes(ROLES.MANAGER)) return 'Manager';
  return 'Employee';
}

/**
 * Premium top application bar.
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
  const navigate = useNavigate();
  // location hook kept for potential future breadcrumb feature
  useLocation();
  const theme = useTheme();
  const isMobileScreen = useMediaQuery(theme.breakpoints.down('md'));

  const [anchorEl, setAnchorEl] = useState(null);
  const menuOpen = Boolean(anchorEl);

  const handleAvatarClick = (e) => setAnchorEl(e.currentTarget);
  const handleMenuClose = () => setAnchorEl(null);

  const handleProfileClick = () => {
    handleMenuClose();
    navigate(ROUTES.PROFILE);
  };
  const handleLogout = () => {
    handleMenuClose();
    logout();
  };

  const initials = user
    ? `${user.firstName?.[0] ?? ''}${user.lastName?.[0] ?? ''}`.toUpperCase()
    : '?';

  const roleLabel = getRoleChipLabel(user?.roles);

  return (
    <AppBar
      position="fixed"
      elevation={0}
      sx={{
        width: { md: `calc(100% - ${sidebarWidth}px)` },
        ml: { md: `${sidebarWidth}px` },
        bgcolor: 'background.paper',
        borderBottom: '1px solid',
        borderColor: 'divider',
        color: 'text.primary',
        backdropFilter: 'blur(8px)',
      }}
    >
      <Toolbar sx={{ gap: 1, minHeight: { xs: 56, sm: 60 }, px: { xs: 2, sm: 3 } }}>
        {/* Mobile menu toggle */}
        <IconButton
          onClick={onMenuClick}
          size="small"
          sx={{
            display: { md: 'none' },
            mr: 0.5,
            color: 'text.secondary',
            bgcolor: 'action.hover',
            borderRadius: '8px',
          }}
          aria-label="Toggle navigation"
        >
          <MenuRoundedIcon fontSize="small" />
        </IconButton>

        {/* Breadcrumb-style page context */}
        <Box sx={{ flexGrow: 1 }} />

        {/* Notification bell */}
        <NotificationBell />

        {/* Theme toggle */}
        <Tooltip title={mode === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'}>
          <IconButton
            onClick={toggleMode}
            size="small"
            sx={{
              color: 'text.secondary',
              bgcolor: 'action.hover',
              borderRadius: '8px',
              width: 34,
              height: 34,
              '&:hover': { bgcolor: 'action.selected' },
            }}
            aria-label="Toggle colour mode"
          >
            {mode === 'dark' ? (
              <LightModeRoundedIcon sx={{ fontSize: 17 }} />
            ) : (
              <DarkModeRoundedIcon sx={{ fontSize: 17 }} />
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
            ml: 0.5,
            cursor: 'pointer',
            borderRadius: '10px',
            px: 1,
            py: 0.5,
            border: '1px solid',
            borderColor: 'divider',
            bgcolor: 'background.default',
            transition: 'all 0.15s ease',
            '&:hover': { borderColor: 'primary.main', bgcolor: 'action.hover' },
          }}
          role="button"
          aria-label="Open account menu"
          aria-haspopup="true"
          aria-expanded={menuOpen}
        >
          <Avatar
            sx={{
              width: 28,
              height: 28,
              background: 'linear-gradient(135deg, #4F46E5, #7C3AED)',
              fontSize: '0.7rem',
              fontWeight: 700,
            }}
          >
            {initials}
          </Avatar>
          {!isMobileScreen && (
            <Box sx={{ minWidth: 0 }}>
              <Typography
                variant="caption"
                fontWeight={700}
                noWrap
                sx={{ display: 'block', lineHeight: 1.2 }}
              >
                {user?.firstName} {user?.lastName}
              </Typography>
              <Typography
                variant="caption"
                color="text.secondary"
                noWrap
                sx={{ fontSize: '0.65rem' }}
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
            }}
          />
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
            sx: { minWidth: 220, mt: 1.5, borderRadius: '14px', overflow: 'visible' },
          }}
        >
          {user && (
            <Box sx={{ px: 2, py: 1.5 }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 0.5 }}>
                <Avatar
                  sx={{
                    width: 38,
                    height: 38,
                    background: 'linear-gradient(135deg, #4F46E5, #7C3AED)',
                    fontSize: '0.8rem',
                    fontWeight: 700,
                  }}
                >
                  {initials}
                </Avatar>
                <Box sx={{ minWidth: 0 }}>
                  <Typography variant="body2" fontWeight={700} noWrap>
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
                  mt: 0.5,
                  height: 20,
                  fontSize: '0.675rem',
                  fontWeight: 700,
                  bgcolor: 'rgba(79,70,229,0.1)',
                  color: 'primary.main',
                  border: '1px solid rgba(79,70,229,0.2)',
                }}
              />
            </Box>
          )}

          <Divider sx={{ my: 0.5 }} />

          <MenuItem onClick={handleProfileClick} sx={{ gap: 1.5 }}>
            <PersonRoundedIcon fontSize="small" sx={{ color: 'text.secondary' }} />
            <Typography variant="body2" fontWeight={500}>
              My Profile
            </Typography>
          </MenuItem>

          <Divider sx={{ my: 0.5 }} />

          <MenuItem onClick={handleLogout} sx={{ gap: 1.5, color: 'error.main', mb: 0.5 }}>
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
