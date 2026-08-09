/**
 * @fileoverview Sidebar navigation component (placeholder).
 *
 * Renders the application navigation drawer. Business page links will be
 * added in Phase 3B once all page components are implemented.
 *
 * @placeholder Navigation items will be expanded in Phase 3B.
 */

import React from 'react';
import { NavLink, useLocation } from 'react-router-dom';
import {
  Box,
  Drawer,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Typography,
  Divider,
  Avatar,
} from '@mui/material';
import DashboardIcon   from '@mui/icons-material/Dashboard';
import PeopleIcon      from '@mui/icons-material/People';
import ApartmentIcon   from '@mui/icons-material/Apartment';
import EventNoteIcon   from '@mui/icons-material/EventNote';
import AccessTimeIcon  from '@mui/icons-material/AccessTime';
import StarRateIcon    from '@mui/icons-material/StarRate';
import PersonIcon      from '@mui/icons-material/Person';
import SettingsIcon    from '@mui/icons-material/Settings';
import { ROUTES } from '@/constants/routes';
import { ROLES } from '@/constants/roles';
import { useAuth } from '@/contexts/AuthContext';

/**
 * @typedef {Object} NavItem
 * @property {string}      label - Display label.
 * @property {string}      path  - Route path.
 * @property {JSX.Element} icon  - MUI icon element.
 */

/**
 * @typedef {Object} NavItem
 * @property {string}      label        - Display label.
 * @property {string}      path         - Route path.
 * @property {JSX.Element} icon         - MUI icon element.
 * @property {string[]}    [allowedRoles] - Roles that can see this item; undefined = all roles.
 */

/** @type {NavItem[]} */
const NAV_ITEMS = [
  {
    label: 'Dashboard',
    path: ROUTES.DASHBOARD,
    icon: <DashboardIcon />,
  },
  {
    label: 'Employees',
    path: ROUTES.EMPLOYEES,
    icon: <PeopleIcon />,
    // All authenticated roles can view employees (EMPLOYEE sees own record)
  },
  {
    label: 'Departments',
    path: ROUTES.DEPARTMENTS,
    icon: <ApartmentIcon />,
    // All roles can view departments (read-only for MANAGER/EMPLOYEE)
  },
  {
    label: 'All Leaves',
    path: ROUTES.LEAVES,
    icon: <EventNoteIcon />,
    allowedRoles: [ROLES.ADMIN, ROLES.HR, ROLES.MANAGER],
  },
  {
    label: 'My Leaves',
    path: ROUTES.MY_LEAVES,
    icon: <EventNoteIcon />,
    allowedRoles: [ROLES.EMPLOYEE],
  },
  {
    label: 'Attendance',
    path: ROUTES.ATTENDANCE,
    icon: <AccessTimeIcon />,
  },
  {
    label: 'Reviews',
    path: ROUTES.REVIEWS,
    icon: <StarRateIcon />,
  },
];

/** @type {NavItem[]} */
const BOTTOM_ITEMS = [
  { label: 'Profile',  path: ROUTES.PROFILE,  icon: <PersonIcon /> },
  { label: 'Settings', path: ROUTES.SETTINGS, icon: <SettingsIcon /> },
];

/**
 * Sidebar navigation drawer.
 *
 * @param {{
 *   open: boolean,
 *   onClose: () => void,
 *   width: number,
 *   variant: 'permanent' | 'temporary'
 * }} props
 * @returns {JSX.Element}
 */
export default function Sidebar({ open, onClose, width, variant }) {
  const location = useLocation();
  const { user, hasAnyRole } = useAuth();

  /**
   * Filters NAV_ITEMS to only those the current user's role is allowed to see.
   *
   * @type {NavItem[]}
   */
  const visibleNavItems = NAV_ITEMS.filter(({ allowedRoles }) => {
    if (!allowedRoles) return true;            // no restriction — show to all
    return hasAnyRole(allowedRoles);
  });

  const content = (
    <Box
      sx={{
        width,
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        bgcolor: 'background.paper',
      }}
    >
      {/* Brand header */}
      <Box sx={{ p: 3, pb: 2 }}>
        <Typography variant="h6" fontWeight={700} color="primary.main" noWrap>
          EMP Portal
        </Typography>
        <Typography variant="caption" color="text.secondary">
          Management System
        </Typography>
      </Box>

      <Divider />

      {/* User info */}
      {user && (
        <Box sx={{ px: 2, py: 1.5, display: 'flex', alignItems: 'center', gap: 1.5 }}>
          <Avatar sx={{ width: 36, height: 36, bgcolor: 'primary.main', fontSize: '0.875rem' }}>
            {user.firstName?.[0]}{user.lastName?.[0]}
          </Avatar>
          <Box sx={{ minWidth: 0 }}>
            <Typography variant="body2" fontWeight={600} noWrap>
              {user.firstName} {user.lastName}
            </Typography>
            <Typography variant="caption" color="text.secondary" noWrap>
              {user.email}
            </Typography>
          </Box>
        </Box>
      )}

      <Divider />

      {/* Main navigation */}
      <List sx={{ px: 1.5, py: 1, flexGrow: 1 }}>
        {visibleNavItems.map(({ label, path, icon }) => (
          <ListItem key={path} disablePadding sx={{ mb: 0.5 }}>
            <ListItemButton
              component={NavLink}
              to={path}
              selected={location.pathname === path}
              onClick={variant === 'temporary' ? onClose : undefined}
              sx={{
                borderRadius: 2,
                '&.active, &.Mui-selected': {
                  bgcolor: 'primary.main',
                  color: 'primary.contrastText',
                  '& .MuiListItemIcon-root': { color: 'primary.contrastText' },
                  '&:hover': { bgcolor: 'primary.dark' },
                },
              }}
            >
              <ListItemIcon sx={{ minWidth: 36 }}>{icon}</ListItemIcon>
              <ListItemText primary={label} primaryTypographyProps={{ fontSize: '0.875rem' }} />
            </ListItemButton>
          </ListItem>
        ))}
      </List>

      <Divider />

      {/* Bottom items */}
      <List sx={{ px: 1.5, py: 1 }}>
        {BOTTOM_ITEMS.map(({ label, path, icon }) => (
          <ListItem key={path} disablePadding sx={{ mb: 0.5 }}>
            <ListItemButton
              component={NavLink}
              to={path}
              selected={location.pathname === path}
              onClick={variant === 'temporary' ? onClose : undefined}
              sx={{ borderRadius: 2 }}
            >
              <ListItemIcon sx={{ minWidth: 36 }}>{icon}</ListItemIcon>
              <ListItemText primary={label} primaryTypographyProps={{ fontSize: '0.875rem' }} />
            </ListItemButton>
          </ListItem>
        ))}
      </List>
    </Box>
  );

  return (
    <Drawer
      variant={variant}
      open={variant === 'permanent' ? true : open}
      onClose={onClose}
      sx={{
        width,
        flexShrink: 0,
        '& .MuiDrawer-paper': {
          width,
          boxSizing: 'border-box',
          border: 'none',
          boxShadow: variant === 'permanent' ? 'none' : undefined,
        },
      }}
    >
      {content}
    </Drawer>
  );
}
