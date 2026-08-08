/**
 * @fileoverview Topbar application bar component.
 *
 * Renders the fixed top application bar with:
 * - Hamburger menu toggle (mobile)
 * - Page title area
 * - Dark/light mode toggle
 * - User avatar menu with logout
 */

import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  AppBar,
  Avatar,
  Box,
  IconButton,
  Menu,
  MenuItem,
  Toolbar,
  Tooltip,
  Typography,
  Divider,
} from '@mui/material';
import MenuIcon          from '@mui/icons-material/Menu';
import LightModeIcon     from '@mui/icons-material/LightMode';
import DarkModeIcon      from '@mui/icons-material/DarkMode';
import LogoutIcon        from '@mui/icons-material/Logout';
import PersonIcon        from '@mui/icons-material/Person';
import { useAuth } from '@/contexts/AuthContext';
import { useThemeMode } from '@/theme/ThemeContext';
import { ROUTES } from '@/constants/routes';

/**
 * The fixed application top bar.
 *
 * @param {{
 *   onMenuClick: () => void,
 *   sidebarWidth: number,
 * }} props
 * @returns {JSX.Element}
 */
export default function Topbar({ onMenuClick, sidebarWidth }) {
  const { user, logout } = useAuth();
  const { mode, toggleMode } = useThemeMode();
  const navigate = useNavigate();

  const [anchorEl, setAnchorEl] = useState(/** @type {HTMLElement|null} */ (null));
  const menuOpen = Boolean(anchorEl);

  /** @param {React.MouseEvent<HTMLElement>} event */
  const handleAvatarClick = (event) => setAnchorEl(event.currentTarget);
  const handleMenuClose   = () => setAnchorEl(null);

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

  return (
    <AppBar
      position="fixed"
      elevation={0}
      sx={{
        width: { md: `calc(100% - ${sidebarWidth}px)` },
        ml:    { md: `${sidebarWidth}px` },
        bgcolor: 'background.paper',
        borderBottom: '1px solid',
        borderColor: 'divider',
        color: 'text.primary',
      }}
    >
      <Toolbar>
        {/* Mobile menu toggle */}
        <IconButton
          edge="start"
          onClick={onMenuClick}
          sx={{ mr: 2, display: { md: 'none' } }}
          aria-label="Toggle navigation menu"
        >
          <MenuIcon />
        </IconButton>

        {/* Spacer */}
        <Box sx={{ flexGrow: 1 }} />

        {/* Dark / light mode toggle */}
        <Tooltip title={mode === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'}>
          <IconButton onClick={toggleMode} aria-label="Toggle colour mode">
            {mode === 'dark' ? <LightModeIcon /> : <DarkModeIcon />}
          </IconButton>
        </Tooltip>

        {/* User avatar */}
        <Tooltip title="Account menu">
          <IconButton onClick={handleAvatarClick} sx={{ ml: 1 }} aria-label="Open account menu">
            <Avatar sx={{ width: 36, height: 36, bgcolor: 'primary.main', fontSize: '0.875rem' }}>
              {initials}
            </Avatar>
          </IconButton>
        </Tooltip>

        {/* User dropdown menu */}
        <Menu
          anchorEl={anchorEl}
          open={menuOpen}
          onClose={handleMenuClose}
          transformOrigin={{ horizontal: 'right', vertical: 'top' }}
          anchorOrigin={{ horizontal: 'right', vertical: 'bottom' }}
          PaperProps={{ elevation: 2, sx: { minWidth: 200, mt: 1, borderRadius: 2 } }}
        >
          {user && (
            <Box sx={{ px: 2, py: 1.5 }}>
              <Typography variant="body2" fontWeight={600}>
                {user.firstName} {user.lastName}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                {user.email}
              </Typography>
            </Box>
          )}
          <Divider />
          <MenuItem onClick={handleProfileClick}>
            <PersonIcon fontSize="small" sx={{ mr: 1.5 }} />
            Profile
          </MenuItem>
          <MenuItem onClick={handleLogout} sx={{ color: 'error.main' }}>
            <LogoutIcon fontSize="small" sx={{ mr: 1.5 }} />
            Logout
          </MenuItem>
        </Menu>
      </Toolbar>
    </AppBar>
  );
}
