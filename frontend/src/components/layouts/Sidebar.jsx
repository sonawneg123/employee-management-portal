/**
 * @fileoverview Sidebar — premium dark navigation drawer.
 *
 * Dark sidebar (#0F172A) with Indigo active state.
 * Role-aware navigation — each role sees only its own routes.
 * Supports: permanent (desktop), temporary (mobile drawer), collapsed icon-only state.
 *
 * Role → routes mapping:
 *  ADMIN    → /admin/*
 *  HR/MGR   → /hr/*
 *  EMPLOYEE → /employee/*
 */

import React from 'react';
import { NavLink, useLocation } from 'react-router-dom';
import {
  Avatar,
  Box,
  Drawer,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Tooltip,
  Typography,
} from '@mui/material';
import DashboardRoundedIcon from '@mui/icons-material/DashboardRounded';
import PeopleRoundedIcon from '@mui/icons-material/PeopleRounded';
import ApartmentRoundedIcon from '@mui/icons-material/ApartmentRounded';
import EventNoteRoundedIcon from '@mui/icons-material/EventNoteRounded';
import AccessTimeRoundedIcon from '@mui/icons-material/AccessTimeRounded';
import PersonRoundedIcon from '@mui/icons-material/PersonRounded';
import SettingsRoundedIcon from '@mui/icons-material/SettingsRounded';
import AssessmentRoundedIcon from '@mui/icons-material/AssessmentRounded';
import ManageAccountsRoundedIcon from '@mui/icons-material/ManageAccountsRounded';
import PeopleAltIcon from '@mui/icons-material/PeopleAlt';
import ChevronLeftRoundedIcon from '@mui/icons-material/ChevronLeftRounded';
import ChevronRightRoundedIcon from '@mui/icons-material/ChevronRightRounded';
import SmartToyRoundedIcon from '@mui/icons-material/SmartToyRounded';
import AutoStoriesRoundedIcon from '@mui/icons-material/AutoStoriesRounded';
import TaskRoundedIcon from '@mui/icons-material/TaskRounded';
import LockRoundedIcon from '@mui/icons-material/LockRounded';
import HowToRegRoundedIcon from '@mui/icons-material/HowToRegRounded';

import { ROUTES } from '@/constants/routes';
import { ROLES } from '@/constants/roles';
import { useAuth } from '@/contexts/AuthContext';
import { useTodayAttendance } from '@/hooks/useAttendanceStatus';

/** Sidebar background colour — intentionally hardcoded to avoid theme leakage. */
const SIDEBAR_BG = '#0F172A';
const SIDEBAR_BORDER = 'rgba(241,245,249,0.06)';
const ACTIVE_BG = 'rgba(79,70,229,0.2)';
const ACTIVE_COLOR = '#818CF8';
const HOVER_BG = 'rgba(241,245,249,0.06)';
const TEXT_MUTED = 'rgba(148,163,184,0.9)';
const TEXT_COLOR = 'rgba(226,232,240,0.9)';

/**
 * @typedef {Object} NavItem
 * @property {string}      label
 * @property {string}      path
 * @property {JSX.Element} icon
 * @property {string[]}    [allowedRoles]
 */

/** @type {NavItem[]} */
const NAV_ITEMS = [
  // ── Admin ─────────────────────────────────────────────────────────────────
  {
    label: 'Dashboard',
    path: ROUTES.ADMIN_DASHBOARD,
    icon: <DashboardRoundedIcon />,
    allowedRoles: [ROLES.ADMIN],
  },
  {
    label: 'Employees',
    path: ROUTES.ADMIN_EMPLOYEES,
    icon: <PeopleRoundedIcon />,
    allowedRoles: [ROLES.ADMIN],
  },
  {
    label: 'Departments',
    path: ROUTES.ADMIN_DEPARTMENTS,
    icon: <ApartmentRoundedIcon />,
    allowedRoles: [ROLES.ADMIN],
  },
  {
    label: 'Leaves',
    path: ROUTES.ADMIN_LEAVES,
    icon: <EventNoteRoundedIcon />,
    allowedRoles: [ROLES.ADMIN],
  },
  {
    label: 'Attendance',
    path: ROUTES.ADMIN_ATTENDANCE,
    icon: <AccessTimeRoundedIcon />,
    allowedRoles: [ROLES.ADMIN],
  },
  {
    label: 'Reviews',
    path: ROUTES.ADMIN_REVIEWS,
    icon: <AssessmentRoundedIcon />,
    allowedRoles: [ROLES.ADMIN],
  },
  {
    label: 'Users',
    path: ROUTES.ADMIN_USERS,
    icon: <ManageAccountsRoundedIcon />,
    allowedRoles: [ROLES.ADMIN],
  },
  {
    label: 'Company Policies',
    path: ROUTES.ADMIN_POLICIES,
    icon: <AutoStoriesRoundedIcon />,
    allowedRoles: [ROLES.ADMIN],
  },

  // ── HR / Manager ──────────────────────────────────────────────────────────
  {
    label: 'Dashboard',
    path: ROUTES.HR_DASHBOARD,
    icon: <DashboardRoundedIcon />,
    allowedRoles: [ROLES.HR, ROLES.MANAGER],
  },
  {
    label: 'Employees',
    path: ROUTES.HR_EMPLOYEES,
    icon: <PeopleRoundedIcon />,
    allowedRoles: [ROLES.HR, ROLES.MANAGER],
  },
  {
    label: 'Departments',
    path: ROUTES.HR_DEPARTMENTS,
    icon: <ApartmentRoundedIcon />,
    allowedRoles: [ROLES.HR, ROLES.MANAGER],
  },
  {
    label: 'Leaves',
    path: ROUTES.HR_LEAVES,
    icon: <EventNoteRoundedIcon />,
    allowedRoles: [ROLES.HR, ROLES.MANAGER],
  },
  {
    label: 'Attendance',
    path: ROUTES.HR_ATTENDANCE,
    icon: <AccessTimeRoundedIcon />,
    allowedRoles: [ROLES.HR, ROLES.MANAGER],
  },
  {
    label: 'Reviews',
    path: ROUTES.HR_REVIEWS,
    icon: <AssessmentRoundedIcon />,
    allowedRoles: [ROLES.HR, ROLES.MANAGER],
  },
  {
    label: 'Leave Approvals',
    path: ROUTES.MANAGER_LEAVES,
    icon: <HowToRegRoundedIcon />,
    allowedRoles: [ROLES.MANAGER],
  },
  {
    label: 'Task Management',
    path: ROUTES.MANAGER_TASKS,
    icon: <TaskRoundedIcon />,
    allowedRoles: [ROLES.HR, ROLES.MANAGER],
  },
  {
    label: 'Task Reviews',
    path: ROUTES.MANAGER_TASK_REVIEWS,
    icon: <PeopleAltIcon />,
    allowedRoles: [ROLES.HR, ROLES.MANAGER],
  },
  {
    label: 'Company Policies',
    path: ROUTES.HR_POLICIES,
    icon: <AutoStoriesRoundedIcon />,
    allowedRoles: [ROLES.HR],
  },

  // ── Employee ──────────────────────────────────────────────────────────────
  {
    label: 'Dashboard',
    path: ROUTES.EMPLOYEE_DASHBOARD,
    icon: <DashboardRoundedIcon />,
    allowedRoles: [ROLES.EMPLOYEE],
  },
  {
    label: 'My Leaves',
    path: ROUTES.EMPLOYEE_LEAVES,
    icon: <EventNoteRoundedIcon />,
    allowedRoles: [ROLES.EMPLOYEE],
  },
  {
    label: 'My Attendance',
    path: ROUTES.EMPLOYEE_ATTENDANCE,
    icon: <AccessTimeRoundedIcon />,
    allowedRoles: [ROLES.EMPLOYEE],
  },
  {
    label: 'My Reviews',
    path: ROUTES.EMPLOYEE_REVIEWS,
    icon: <AssessmentRoundedIcon />,
    allowedRoles: [ROLES.EMPLOYEE],
  },
  {
    label: 'My Tasks',
    path: ROUTES.EMPLOYEE_TASKS,
    icon: <TaskRoundedIcon />,
    allowedRoles: [ROLES.EMPLOYEE],
  },
  {
    label: 'My Profile',
    path: ROUTES.EMPLOYEE_PROFILE,
    icon: <PersonRoundedIcon />,
    allowedRoles: [ROLES.EMPLOYEE],
  },
];

/** @type {NavItem[]} */
const BOTTOM_ITEMS = [
  {
    label: 'AI Assistant',
    path: ROUTES.AI_ASSISTANT,
    icon: <SmartToyRoundedIcon />,
  },
  { label: 'Profile', path: ROUTES.PROFILE, icon: <PersonRoundedIcon /> },
  { label: 'Settings', path: ROUTES.SETTINGS, icon: <SettingsRoundedIcon /> },
];

/**
 * Returns the display role label for the user avatar row.
 *
 * @param {string[]} roles
 * @returns {string}
 */
function roleLabel(roles) {
  if (!roles?.length) return 'User';
  if (roles.includes(ROLES.ADMIN)) return 'Administrator';
  if (roles.includes(ROLES.HR)) return 'HR Manager';
  if (roles.includes(ROLES.MANAGER)) return 'Manager';
  return 'Employee';
}

/**
 * Sidebar navigation drawer.
 *
 * @param {{
 *   open:             boolean,
 *   onClose:          () => void,
 *   onCollapseToggle: () => void,
 *   width:            number,
 *   collapsedWidth:   number,
 *   collapsed:        boolean,
 *   variant:          'permanent' | 'temporary',
 * }} props
 * @returns {JSX.Element}
 */
export default function Sidebar({
  open,
  onClose,
  onCollapseToggle,
  width,
  collapsedWidth,
  collapsed,
  variant,
}) {
  const location = useLocation();
  const { user, hasAnyRole } = useAuth();
  const { isCheckedOut } = useTodayAttendance();

  const isEmployee = Boolean(user)
    && hasAnyRole([ROLES.EMPLOYEE])
    && !hasAnyRole([ROLES.HR, ROLES.MANAGER, ROLES.ADMIN]);

  const visibleNavItems = NAV_ITEMS.filter(
    ({ allowedRoles }) => !allowedRoles || hasAnyRole(allowedRoles),
  );

  const initials = user
    ? `${user.firstName?.[0] ?? ''}${user.lastName?.[0] ?? ''}`.toUpperCase()
    : '?';

  // Effective width based on collapsed state (only for permanent variant)
  const effectiveWidth = variant === 'permanent' && collapsed ? collapsedWidth : width;

  const content = (
    <Box
      sx={{
        width: effectiveWidth,
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        bgcolor: SIDEBAR_BG,
        borderRight: `1px solid ${SIDEBAR_BORDER}`,
        overflow: 'hidden',
        transition: 'width 0.25s ease',
      }}
    >
      {/* ── Brand ──────────────────────────────────────────────────────── */}
      <Box
        sx={{
          px: collapsed ? 1 : 2.5,
          pt: 2.5,
          pb: 2,
          display: 'flex',
          alignItems: 'center',
          gap: collapsed ? 0 : 1.25,
          borderBottom: `1px solid ${SIDEBAR_BORDER}`,
          justifyContent: collapsed ? 'center' : 'flex-start',
          transition: 'padding 0.25s ease',
        }}
      >
        <Box
          sx={{
            width: 32,
            height: 32,
            borderRadius: '8px',
            background: 'linear-gradient(135deg, #4F46E5, #7C3AED)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            flexShrink: 0,
          }}
        >
          <PeopleAltIcon sx={{ color: '#fff', fontSize: 17 }} />
        </Box>
        {!collapsed && (
          <Box sx={{ minWidth: 0 }}>
            <Typography
              variant="body2"
              fontWeight={800}
              noWrap
              sx={{ color: '#F1F5F9', letterSpacing: '-0.01em', lineHeight: 1.2 }}
            >
              PeopleCore HR
            </Typography>
            <Typography variant="caption" sx={{ color: TEXT_MUTED, fontSize: '0.675rem' }}>
              Management Portal
            </Typography>
          </Box>
        )}
      </Box>

      {/* ── User info ──────────────────────────────────────────────────── */}
      {user && !collapsed && (
        <Box
          sx={{
            px: 2.5,
            py: 2,
            borderBottom: `1px solid ${SIDEBAR_BORDER}`,
          }}
        >
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
            <Avatar
              sx={{
                width: 34,
                height: 34,
                background: 'linear-gradient(135deg, #4F46E5, #7C3AED)',
                fontSize: '0.75rem',
                fontWeight: 700,
                flexShrink: 0,
              }}
            >
              {initials}
            </Avatar>
            <Box sx={{ minWidth: 0 }}>
              <Typography
                variant="body2"
                fontWeight={600}
                noWrap
                sx={{ color: '#F1F5F9', fontSize: '0.8rem' }}
              >
                {user.firstName} {user.lastName}
              </Typography>
              <Typography
                variant="caption"
                noWrap
                sx={{ color: TEXT_MUTED, fontSize: '0.7rem', display: 'block' }}
              >
                {roleLabel(user.roles)}
              </Typography>
            </Box>
          </Box>
        </Box>
      )}

      {/* Collapsed user avatar only */}
      {user && collapsed && (
        <Box
          sx={{
            py: 1.5,
            display: 'flex',
            justifyContent: 'center',
            borderBottom: `1px solid ${SIDEBAR_BORDER}`,
          }}
        >
          <Tooltip title={`${user.firstName} ${user.lastName}`} placement="right">
            <Avatar
              sx={{
                width: 32,
                height: 32,
                background: 'linear-gradient(135deg, #4F46E5, #7C3AED)',
                fontSize: '0.7rem',
                fontWeight: 700,
                cursor: 'default',
              }}
            >
              {initials}
            </Avatar>
          </Tooltip>
        </Box>
      )}

      {/* ── Main nav ───────────────────────────────────────────────────── */}
      <Box
        sx={{
          px: collapsed ? 0.5 : 1.5,
          py: 1.5,
          flexGrow: 1,
          overflowY: 'auto',
          overflowX: 'hidden',
        }}
      >
        {!collapsed && (
          <Typography
            variant="overline"
            sx={{
              color: TEXT_MUTED,
              fontSize: '0.6rem',
              px: 1,
              mb: 1,
              display: 'block',
            }}
          >
            Navigation
          </Typography>
        )}
        <List disablePadding>
          {visibleNavItems.map(({ label, path, icon }) => {
            const isActive = location.pathname === path || location.pathname.startsWith(path + '/');

            // Disable the employee Tasks item when the employee has checked out
            const isTasksItem = path === ROUTES.EMPLOYEE_TASKS;
            const isDisabled = isEmployee && isTasksItem && isCheckedOut;

            const tooltipTitle = collapsed
              ? isDisabled ? `${label} (Checked out — check in to access tasks)` : label
              : isDisabled ? 'You have checked out. Check in again to access tasks.' : '';

            return (
              <ListItem key={path} disablePadding sx={{ mb: 0.25 }}>
                <Tooltip
                  title={tooltipTitle}
                  placement="right"
                  disableHoverListener={!isDisabled && !collapsed}
                >
                  {/* span wrapper needed so Tooltip works on disabled elements */}
                  <span style={{ display: 'block', width: '100%' }}>
                    <ListItemButton
                      component={isDisabled ? 'div' : NavLink}
                      to={isDisabled ? undefined : path}
                      onClick={
                        isDisabled ? undefined
                          : variant === 'temporary' ? onClose : undefined
                      }
                      sx={{
                        borderRadius: '10px',
                        py: 0.85,
                        px: collapsed ? 0 : 1.25,
                        minHeight: 40,
                        justifyContent: collapsed ? 'center' : 'flex-start',
                        color: isDisabled
                          ? 'rgba(148,163,184,0.3)'
                          : isActive ? ACTIVE_COLOR : TEXT_COLOR,
                        bgcolor: isActive && !isDisabled ? ACTIVE_BG : 'transparent',
                        opacity: isDisabled ? 0.5 : 1,
                        cursor: isDisabled ? 'not-allowed' : 'pointer',
                        pointerEvents: isDisabled ? 'none' : 'auto',
                        '&:hover': isDisabled ? {} : {
                          bgcolor: isActive ? ACTIVE_BG : HOVER_BG,
                          color: isActive ? ACTIVE_COLOR : '#F1F5F9',
                        },
                        transition: 'all 0.15s ease',
                      }}
                      aria-current={isActive && !isDisabled ? 'page' : undefined}
                      aria-disabled={isDisabled}
                    >
                      <ListItemIcon
                        sx={{
                          minWidth: collapsed ? 0 : 34,
                          color: isDisabled
                            ? 'rgba(148,163,184,0.3)'
                            : isActive ? ACTIVE_COLOR : TEXT_MUTED,
                          '& .MuiSvgIcon-root': { fontSize: 18 },
                          justifyContent: 'center',
                        }}
                      >
                        {icon}
                      </ListItemIcon>
                      {!collapsed && (
                        <>
                          <ListItemText
                            primary={label}
                            primaryTypographyProps={{
                              fontSize: '0.8375rem',
                              fontWeight: isActive ? 600 : 500,
                              letterSpacing: '0.005em',
                            }}
                          />
                          {isDisabled && (
                            <LockRoundedIcon
                              sx={{
                                fontSize: 14,
                                color: 'rgba(148,163,184,0.35)',
                                ml: 1,
                                flexShrink: 0,
                              }}
                            />
                          )}
                          {isActive && !isDisabled && (
                            <Box
                              sx={{
                                width: 3,
                                height: 20,
                                borderRadius: '3px',
                                background: 'linear-gradient(180deg, #4F46E5, #7C3AED)',
                                ml: 1,
                                flexShrink: 0,
                              }}
                            />
                          )}
                        </>
                      )}
                    </ListItemButton>
                  </span>
                </Tooltip>
              </ListItem>
            );
          })}
        </List>
      </Box>

      {/* ── Bottom items ───────────────────────────────────────────────── */}
      <Box
        sx={{
          px: collapsed ? 0.5 : 1.5,
          py: 1.5,
          borderTop: `1px solid ${SIDEBAR_BORDER}`,
        }}
      >
        <List disablePadding>
          {BOTTOM_ITEMS.map(({ label, path, icon }) => {
            const isActive = location.pathname === path;
            return (
              <ListItem key={path} disablePadding sx={{ mb: 0.25 }}>
                <Tooltip
                  title={collapsed ? label : ''}
                  placement="right"
                  disableHoverListener={!collapsed}
                >
                  <ListItemButton
                    component={NavLink}
                    to={path}
                    onClick={variant === 'temporary' ? onClose : undefined}
                    sx={{
                      borderRadius: '10px',
                      py: 0.85,
                      px: collapsed ? 0 : 1.25,
                      minHeight: 38,
                      justifyContent: collapsed ? 'center' : 'flex-start',
                      color: isActive ? ACTIVE_COLOR : TEXT_COLOR,
                      bgcolor: isActive ? ACTIVE_BG : 'transparent',
                      '&:hover': { bgcolor: HOVER_BG, color: '#F1F5F9' },
                      transition: 'all 0.15s ease',
                    }}
                    aria-current={isActive ? 'page' : undefined}
                  >
                    <ListItemIcon
                      sx={{
                        minWidth: collapsed ? 0 : 34,
                        color: isActive ? ACTIVE_COLOR : TEXT_MUTED,
                        '& .MuiSvgIcon-root': { fontSize: 18 },
                        justifyContent: 'center',
                      }}
                    >
                      {icon}
                    </ListItemIcon>
                    {!collapsed && (
                      <ListItemText
                        primary={label}
                        primaryTypographyProps={{
                          fontSize: '0.8375rem',
                          fontWeight: isActive ? 600 : 500,
                        }}
                      />
                    )}
                  </ListItemButton>
                </Tooltip>
              </ListItem>
            );
          })}
        </List>

        {/* Collapse toggle (desktop only) */}
        {variant === 'permanent' && (
          <Tooltip title={collapsed ? 'Expand sidebar' : 'Collapse sidebar'} placement="right">
            <ListItemButton
              onClick={onCollapseToggle}
              sx={{
                borderRadius: '10px',
                py: 0.75,
                px: collapsed ? 0 : 1.25,
                minHeight: 36,
                justifyContent: collapsed ? 'center' : 'flex-start',
                color: TEXT_MUTED,
                '&:hover': { bgcolor: HOVER_BG, color: '#F1F5F9' },
                transition: 'all 0.15s ease',
                mt: 0.5,
              }}
              aria-label={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
            >
              <ListItemIcon
                sx={{
                  minWidth: collapsed ? 0 : 34,
                  color: 'inherit',
                  '& .MuiSvgIcon-root': { fontSize: 18 },
                  justifyContent: 'center',
                }}
              >
                {collapsed ? <ChevronRightRoundedIcon /> : <ChevronLeftRoundedIcon />}
              </ListItemIcon>
              {!collapsed && (
                <ListItemText
                  primary="Collapse"
                  primaryTypographyProps={{ fontSize: '0.8375rem', fontWeight: 500 }}
                />
              )}
            </ListItemButton>
          </Tooltip>
        )}
      </Box>
    </Box>
  );

  return (
    <Drawer
      variant={variant}
      open={variant === 'permanent' ? true : open}
      onClose={onClose}
      sx={{
        width: effectiveWidth,
        flexShrink: 0,
        transition: 'width 0.25s ease',
        '& .MuiDrawer-paper': {
          width: effectiveWidth,
          boxSizing: 'border-box',
          border: 'none',
          bgcolor: SIDEBAR_BG,
          transition: 'width 0.25s ease',
          overflowX: 'hidden',
        },
      }}
    >
      {content}
    </Drawer>
  );
}
