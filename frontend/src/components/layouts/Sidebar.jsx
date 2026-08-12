/**
 * @fileoverview Sidebar navigation component.
 *
 * Renders the application navigation drawer with role-aware menu items.
 * Each role sees only the navigation items appropriate to their access level,
 * pointing at the correct role-scoped route paths.
 *
 * Role → nav items:
 * - ADMIN    → /admin/dashboard, /admin/employees, /admin/departments, /admin/leaves, /admin/attendance
 * - HR       → /hr/dashboard, /hr/employees, /hr/leaves, /hr/attendance
 * - MANAGER  → /hr/dashboard, /hr/employees, /hr/leaves, /hr/attendance
 * - EMPLOYEE → /employee/dashboard, /employee/leaves, /employee/attendance, /employee/profile
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
import DashboardIcon        from '@mui/icons-material/Dashboard';
import PeopleIcon           from '@mui/icons-material/People';
import ApartmentIcon        from '@mui/icons-material/Apartment';
import EventNoteIcon        from '@mui/icons-material/EventNote';
import AccessTimeIcon       from '@mui/icons-material/AccessTime';
import PersonIcon           from '@mui/icons-material/Person';
import SettingsIcon         from '@mui/icons-material/Settings';
import AssessmentIcon       from '@mui/icons-material/Assessment';
import ManageAccountsIcon   from '@mui/icons-material/ManageAccounts';
import { ROUTES } from '@/constants/routes';
import { ROLES } from '@/constants/roles';
import { useAuth } from '@/contexts/AuthContext';

/**
 * @typedef {Object} NavItem
 * @property {string}      label          - Display label.
 * @property {string}      path           - Route path.
 * @property {JSX.Element} icon           - MUI icon element.
 * @property {string[]}    [allowedRoles] - Roles that can see this item; undefined = all roles.
 */

/** @type {NavItem[]} */
const NAV_ITEMS = [
  // ── Admin items ─────────────────────────────────────────────────────────────
  {
    label: 'Dashboard',
    path:  ROUTES.ADMIN_DASHBOARD,
    icon:  <DashboardIcon />,
    allowedRoles: [ROLES.ADMIN],
  },
  {
    label: 'Employees',
    path:  ROUTES.ADMIN_EMPLOYEES,
    icon:  <PeopleIcon />,
    allowedRoles: [ROLES.ADMIN],
  },
  {
    label: 'Departments',
    path:  ROUTES.ADMIN_DEPARTMENTS,
    icon:  <ApartmentIcon />,
    allowedRoles: [ROLES.ADMIN],
  },
  {
    label: 'Leaves',
    path:  ROUTES.ADMIN_LEAVES,
    icon:  <EventNoteIcon />,
    allowedRoles: [ROLES.ADMIN],
  },
  {
    label: 'Attendance',
    path:  ROUTES.ADMIN_ATTENDANCE,
    icon:  <AccessTimeIcon />,
    allowedRoles: [ROLES.ADMIN],
  },
  {
    label: 'Reviews',
    path:  ROUTES.ADMIN_REVIEWS,
    icon:  <AssessmentIcon />,
    allowedRoles: [ROLES.ADMIN],
  },
  {
    label: 'Users',
    path:  ROUTES.ADMIN_USERS,
    icon:  <ManageAccountsIcon />,
    allowedRoles: [ROLES.ADMIN],
  },

  // ── HR / Manager items ───────────────────────────────────────────────────────
  {
    label: 'Dashboard',
    path:  ROUTES.HR_DASHBOARD,
    icon:  <DashboardIcon />,
    allowedRoles: [ROLES.HR, ROLES.MANAGER],
  },
  {
    label: 'Employees',
    path:  ROUTES.HR_EMPLOYEES,
    icon:  <PeopleIcon />,
    allowedRoles: [ROLES.HR, ROLES.MANAGER],
  },
  {
    label: 'Departments',
    path:  ROUTES.HR_DEPARTMENTS,
    icon:  <ApartmentIcon />,
    allowedRoles: [ROLES.HR, ROLES.MANAGER],
  },
  {
    label: 'Leaves',
    path:  ROUTES.HR_LEAVES,
    icon:  <EventNoteIcon />,
    allowedRoles: [ROLES.HR, ROLES.MANAGER],
  },
  {
    label: 'Attendance',
    path:  ROUTES.HR_ATTENDANCE,
    icon:  <AccessTimeIcon />,
    allowedRoles: [ROLES.HR, ROLES.MANAGER],
  },
  {
    label: 'Reviews',
    path:  ROUTES.HR_REVIEWS,
    icon:  <AssessmentIcon />,
    allowedRoles: [ROLES.HR, ROLES.MANAGER],
  },

  // ── Employee items ───────────────────────────────────────────────────────────
  {
    label: 'Dashboard',
    path:  ROUTES.EMPLOYEE_DASHBOARD,
    icon:  <DashboardIcon />,
    allowedRoles: [ROLES.EMPLOYEE],
  },
  {
    label: 'My Leaves',
    path:  ROUTES.EMPLOYEE_LEAVES,
    icon:  <EventNoteIcon />,
    allowedRoles: [ROLES.EMPLOYEE],
  },
  {
    label: 'My Attendance',
    path:  ROUTES.EMPLOYEE_ATTENDANCE,
    icon:  <AccessTimeIcon />,
    allowedRoles: [ROLES.EMPLOYEE],
  },
  {
    label: 'My Reviews',
    path:  ROUTES.EMPLOYEE_REVIEWS,
    icon:  <AssessmentIcon />,
    allowedRoles: [ROLES.EMPLOYEE],
  },
  {
    label: 'My Profile',
    path:  ROUTES.EMPLOYEE_PROFILE,
    icon:  <PersonIcon />,
    allowedRoles: [ROLES.EMPLOYEE],
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
    if (!allowedRoles) return true;
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
