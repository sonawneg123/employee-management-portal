/**
 * @fileoverview Sidebar — modern light navigation drawer for PeopleCore HR.
 *
 * White sidebar with indigo active state and subtle dividers.
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
  useTheme,
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
import HowToRegRoundedIcon from '@mui/icons-material/HowToRegRounded';

import { ROUTES } from '@/constants/routes';
import { ROLES } from '@/constants/roles';
import { useAuth } from '@/contexts/AuthContext';
import { useTodayAttendance } from '@/hooks/useAttendanceStatus';

/**
 * @typedef {Object} NavItem
 * @property {string}      label
 * @property {string}      path
 * @property {JSX.Element} icon
 * @property {string[]}    [allowedRoles]
 * @property {string}      [section]
 */

/** @type {NavItem[]} */
const NAV_ITEMS = [
  // ── Admin ─────────────────────────────────────────────────────────────────
  {
    label: 'Dashboard',
    path: ROUTES.ADMIN_DASHBOARD,
    icon: <DashboardRoundedIcon />,
    allowedRoles: [ROLES.ADMIN],
    section: 'main',
  },
  {
    label: 'Employees',
    path: ROUTES.ADMIN_EMPLOYEES,
    icon: <PeopleRoundedIcon />,
    allowedRoles: [ROLES.ADMIN],
    section: 'main',
  },
  {
    label: 'Departments',
    path: ROUTES.ADMIN_DEPARTMENTS,
    icon: <ApartmentRoundedIcon />,
    allowedRoles: [ROLES.ADMIN],
    section: 'main',
  },
  {
    label: 'Leaves',
    path: ROUTES.ADMIN_LEAVES,
    icon: <EventNoteRoundedIcon />,
    allowedRoles: [ROLES.ADMIN],
    section: 'main',
  },
  {
    label: 'Attendance',
    path: ROUTES.ADMIN_ATTENDANCE,
    icon: <AccessTimeRoundedIcon />,
    allowedRoles: [ROLES.ADMIN],
    section: 'main',
  },
  {
    label: 'Reviews',
    path: ROUTES.ADMIN_REVIEWS,
    icon: <AssessmentRoundedIcon />,
    allowedRoles: [ROLES.ADMIN],
    section: 'main',
  },
  {
    label: 'Users',
    path: ROUTES.ADMIN_USERS,
    icon: <ManageAccountsRoundedIcon />,
    allowedRoles: [ROLES.ADMIN],
    section: 'management',
  },
  {
    label: 'Company Policies',
    path: ROUTES.ADMIN_POLICIES,
    icon: <AutoStoriesRoundedIcon />,
    allowedRoles: [ROLES.ADMIN],
    section: 'management',
  },

  // ── HR / Manager ──────────────────────────────────────────────────────────
  {
    label: 'Dashboard',
    path: ROUTES.HR_DASHBOARD,
    icon: <DashboardRoundedIcon />,
    allowedRoles: [ROLES.HR, ROLES.MANAGER],
    section: 'main',
  },
  {
    label: 'Employees',
    path: ROUTES.HR_EMPLOYEES,
    icon: <PeopleRoundedIcon />,
    allowedRoles: [ROLES.HR, ROLES.MANAGER],
    section: 'main',
  },
  {
    label: 'Departments',
    path: ROUTES.HR_DEPARTMENTS,
    icon: <ApartmentRoundedIcon />,
    allowedRoles: [ROLES.HR, ROLES.MANAGER],
    section: 'main',
  },
  {
    label: 'Leaves',
    path: ROUTES.HR_LEAVES,
    icon: <EventNoteRoundedIcon />,
    allowedRoles: [ROLES.HR, ROLES.MANAGER],
    section: 'main',
  },
  {
    label: 'Attendance',
    path: ROUTES.HR_ATTENDANCE,
    icon: <AccessTimeRoundedIcon />,
    allowedRoles: [ROLES.HR, ROLES.MANAGER],
    section: 'main',
  },
  {
    label: 'Reviews',
    path: ROUTES.HR_REVIEWS,
    icon: <AssessmentRoundedIcon />,
    allowedRoles: [ROLES.HR, ROLES.MANAGER],
    section: 'main',
  },
  {
    label: 'Leave Approvals',
    path: ROUTES.MANAGER_LEAVES,
    icon: <HowToRegRoundedIcon />,
    allowedRoles: [ROLES.MANAGER],
    section: 'management',
  },
  {
    label: 'Task Management',
    path: ROUTES.MANAGER_TASKS,
    icon: <TaskRoundedIcon />,
    allowedRoles: [ROLES.HR, ROLES.MANAGER],
    section: 'management',
  },
  {
    label: 'Task Reviews',
    path: ROUTES.MANAGER_TASK_REVIEWS,
    icon: <PeopleAltIcon />,
    allowedRoles: [ROLES.HR, ROLES.MANAGER],
    section: 'management',
  },
  {
    label: 'Company Policies',
    path: ROUTES.HR_POLICIES,
    icon: <AutoStoriesRoundedIcon />,
    allowedRoles: [ROLES.HR],
    section: 'management',
  },

  // ── Employee ──────────────────────────────────────────────────────────────
  {
    label: 'Dashboard',
    path: ROUTES.EMPLOYEE_DASHBOARD,
    icon: <DashboardRoundedIcon />,
    allowedRoles: [ROLES.EMPLOYEE],
    section: 'main',
  },
  {
    label: 'My Leaves',
    path: ROUTES.EMPLOYEE_LEAVES,
    icon: <EventNoteRoundedIcon />,
    allowedRoles: [ROLES.EMPLOYEE],
    section: 'main',
  },
  {
    label: 'My Attendance',
    path: ROUTES.EMPLOYEE_ATTENDANCE,
    icon: <AccessTimeRoundedIcon />,
    allowedRoles: [ROLES.EMPLOYEE],
    section: 'main',
  },
  {
    label: 'My Reviews',
    path: ROUTES.EMPLOYEE_REVIEWS,
    icon: <AssessmentRoundedIcon />,
    allowedRoles: [ROLES.EMPLOYEE],
    section: 'main',
  },
  {
    label: 'My Tasks',
    path: ROUTES.EMPLOYEE_TASKS,
    icon: <TaskRoundedIcon />,
    allowedRoles: [ROLES.EMPLOYEE],
    section: 'main',
  },
];

/** @type {NavItem[]} */
const ACCOUNT_ITEMS = [
  {
    label: 'AI Assistant',
    path: ROUTES.AI_ASSISTANT,
    icon: <SmartToyRoundedIcon />,
    section: 'ai',
  },
  { label: 'My Profile', path: ROUTES.PROFILE, icon: <PersonRoundedIcon />, section: 'account' },
  {
    label: 'Settings',
    path: ROUTES.SETTINGS,
    icon: <SettingsRoundedIcon />,
    section: 'account',
  },
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
  const theme = useTheme();
  const isDark = theme.palette.mode === 'dark';

  const visibleNavItems = NAV_ITEMS.filter(
    ({ allowedRoles }) => !allowedRoles || hasAnyRole(allowedRoles),
  );

  const mainItems = visibleNavItems.filter((i) => i.section === 'main');
  const mgmtItems = visibleNavItems.filter((i) => i.section === 'management');

  const initials = user
    ? `${user.firstName?.[0] ?? ''}${user.lastName?.[0] ?? ''}`.toUpperCase()
    : '?';

  // Effective width based on collapsed state (only for permanent variant)
  const effectiveWidth = variant === 'permanent' && collapsed ? collapsedWidth : width;

  // Theme-aware colours
  const bg = isDark ? '#111827' : '#FFFFFF';
  const border = isDark ? 'rgba(241,245,249,0.08)' : '#E5E7EB';
  const activeBg = isDark ? 'rgba(79,70,229,0.18)' : '#EEF2FF';
  const activeColor = isDark ? '#818CF8' : '#4F46E5';
  const hoverBg = isDark ? 'rgba(241,245,249,0.06)' : 'rgba(79,70,229,0.05)';
  const textColor = isDark ? 'rgba(226,232,240,0.9)' : '#374151';
  const textMuted = isDark ? 'rgba(148,163,184,0.8)' : '#9CA3AF';
  const sectionLabelColor = isDark ? 'rgba(148,163,184,0.6)' : '#9CA3AF';

  /**
   * Renders a single nav list item.
   *
   * @param {NavItem} item
   * @returns {JSX.Element}
   */
  const renderItem = (item) => {
    const isActive =
      location.pathname === item.path ||
      (item.path !== '/' && location.pathname.startsWith(item.path));

    const btn = (
      <ListItemButton
        component={NavLink}
        to={item.path}
        onClick={variant === 'temporary' ? onClose : undefined}
        selected={isActive}
        sx={{
          borderRadius: '10px',
          mx: 1,
          px: collapsed ? 1 : 1.5,
          py: 0.9,
          minHeight: 40,
          justifyContent: collapsed ? 'center' : 'flex-start',
          bgcolor: isActive ? activeBg : 'transparent',
          color: isActive ? activeColor : textColor,
          '&:hover': {
            bgcolor: isActive ? activeBg : hoverBg,
            color: isActive ? activeColor : isDark ? '#F1F5F9' : '#111827',
          },
          '&.Mui-selected': {
            bgcolor: activeBg,
            '&:hover': { bgcolor: activeBg },
          },
          transition: 'all 0.15s ease',
        }}
        aria-label={item.label}
        aria-current={isActive ? 'page' : undefined}
      >
        <ListItemIcon
          sx={{
            minWidth: collapsed ? 0 : 36,
            color: 'inherit',
            justifyContent: 'center',
            '& svg': { fontSize: 20 },
          }}
        >
          {item.icon}
        </ListItemIcon>
        {!collapsed && (
          <ListItemText
            primary={item.label}
            primaryTypographyProps={{
              fontSize: '0.875rem',
              fontWeight: isActive ? 600 : 500,
              noWrap: true,
            }}
          />
        )}
        {/* Attendance status dot (employees only) */}
        {!collapsed && item.path === ROUTES.EMPLOYEE_ATTENDANCE && isCheckedOut != null && (
          <Box
            sx={{
              width: 7,
              height: 7,
              borderRadius: '50%',
              bgcolor: isCheckedOut ? 'success.main' : 'warning.main',
              flexShrink: 0,
              mr: 0.5,
            }}
            aria-hidden="true"
          />
        )}
      </ListItemButton>
    );

    return (
      <ListItem key={item.path + item.label} disablePadding sx={{ mb: 0.25 }}>
        {collapsed ? (
          <Tooltip title={item.label} placement="right">
            {btn}
          </Tooltip>
        ) : (
          btn
        )}
      </ListItem>
    );
  };

  const content = (
    <Box
      sx={{
        width: effectiveWidth,
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        bgcolor: bg,
        borderRight: `1px solid ${border}`,
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
          borderBottom: `1px solid ${border}`,
          justifyContent: collapsed ? 'center' : 'flex-start',
          transition: 'padding 0.25s ease',
          minHeight: 64,
        }}
      >
        <Box
          sx={{
            width: 34,
            height: 34,
            borderRadius: '10px',
            background: 'linear-gradient(135deg, #4F46E5, #7C3AED)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            flexShrink: 0,
            boxShadow: '0 4px 12px rgba(79,70,229,0.35)',
          }}
          aria-hidden="true"
        >
          <PeopleAltIcon sx={{ color: '#fff', fontSize: 18 }} />
        </Box>
        {!collapsed && (
          <Box sx={{ minWidth: 0 }}>
            <Typography
              variant="body2"
              fontWeight={800}
              noWrap
              sx={{
                color: isDark ? '#F1F5F9' : '#111827',
                letterSpacing: '-0.02em',
                lineHeight: 1.2,
                fontSize: '0.9375rem',
              }}
            >
              PeopleCore HR
            </Typography>
            <Typography variant="caption" sx={{ color: textMuted, fontSize: '0.675rem' }}>
              Management Portal
            </Typography>
          </Box>
        )}
      </Box>

      {/* ── Nav list (scrollable) ───────────────────────────────────────── */}
      <Box sx={{ flexGrow: 1, overflowY: 'auto', overflowX: 'hidden', py: 1.5 }}>
        {/* Main nav section */}
        {!collapsed && (
          <Typography
            variant="overline"
            sx={{
              px: 2.5,
              mb: 0.5,
              display: 'block',
              color: sectionLabelColor,
              fontSize: '0.625rem',
              letterSpacing: '0.1em',
              lineHeight: 2,
            }}
          >
            Main
          </Typography>
        )}
        <List disablePadding>{mainItems.map(renderItem)}</List>

        {/* Management section */}
        {mgmtItems.length > 0 && (
          <>
            {!collapsed && (
              <Typography
                variant="overline"
                sx={{
                  px: 2.5,
                  mt: 1.5,
                  mb: 0.5,
                  display: 'block',
                  color: sectionLabelColor,
                  fontSize: '0.625rem',
                  letterSpacing: '0.1em',
                  lineHeight: 2,
                }}
              >
                Management
              </Typography>
            )}
            {collapsed && <Box sx={{ height: 8 }} />}
            <List disablePadding>{mgmtItems.map(renderItem)}</List>
          </>
        )}

        {/* AI + Account section */}
        {!collapsed && (
          <Typography
            variant="overline"
            sx={{
              px: 2.5,
              mt: 1.5,
              mb: 0.5,
              display: 'block',
              color: sectionLabelColor,
              fontSize: '0.625rem',
              letterSpacing: '0.1em',
              lineHeight: 2,
            }}
          >
            Account
          </Typography>
        )}
        {collapsed && <Box sx={{ height: 8 }} />}
        <List disablePadding>{ACCOUNT_ITEMS.map(renderItem)}</List>
      </Box>

      {/* ── User info + collapse toggle ──────────────────────────────────── */}
      <Box
        sx={{
          borderTop: `1px solid ${border}`,
          p: collapsed ? 1 : 1.5,
          display: 'flex',
          alignItems: 'center',
          gap: 1.25,
          justifyContent: collapsed ? 'center' : 'space-between',
        }}
      >
        {user && !collapsed && (
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, minWidth: 0, flex: 1 }}>
            <Avatar
              src={user.profilePhotoUrl || undefined}
              sx={{
                width: 32,
                height: 32,
                background: 'linear-gradient(135deg, #4F46E5, #7C3AED)',
                fontSize: '0.75rem',
                fontWeight: 700,
                flexShrink: 0,
              }}
            >
              {!user.profilePhotoUrl && initials}
            </Avatar>
            <Box sx={{ minWidth: 0 }}>
              <Typography
                variant="caption"
                fontWeight={700}
                noWrap
                sx={{ display: 'block', color: isDark ? '#F1F5F9' : '#111827', lineHeight: 1.3 }}
              >
                {user.firstName} {user.lastName}
              </Typography>
              <Typography
                variant="caption"
                noWrap
                sx={{ color: textMuted, fontSize: '0.65rem', display: 'block' }}
              >
                {roleLabel(user?.roles)}
              </Typography>
            </Box>
          </Box>
        )}

        {/* Collapse toggle (desktop only) */}
        {variant === 'permanent' && (
          <Tooltip title={collapsed ? 'Expand sidebar' : 'Collapse sidebar'} placement="right">
            <Box
              onClick={onCollapseToggle}
              role="button"
              tabIndex={0}
              onKeyDown={(e) => e.key === 'Enter' && onCollapseToggle()}
              aria-label={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
              sx={{
                width: 28,
                height: 28,
                borderRadius: '8px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                cursor: 'pointer',
                bgcolor: hoverBg,
                color: textMuted,
                flexShrink: 0,
                transition: 'all 0.15s ease',
                '&:hover': { bgcolor: activeBg, color: activeColor },
                '&:focus-visible': { outline: `2px solid ${activeColor}`, outlineOffset: 2 },
              }}
            >
              {collapsed ? (
                <ChevronRightRoundedIcon sx={{ fontSize: 16 }} />
              ) : (
                <ChevronLeftRoundedIcon sx={{ fontSize: 16 }} />
              )}
            </Box>
          </Tooltip>
        )}
      </Box>
    </Box>
  );

  return (
    <Drawer
      variant={variant}
      open={variant === 'temporary' ? open : true}
      onClose={onClose}
      ModalProps={{ keepMounted: true }}
      PaperProps={{
        sx: {
          width: effectiveWidth,
          border: 'none',
          boxShadow:
            variant === 'temporary'
              ? isDark
                ? '4px 0 24px rgba(0,0,0,0.5)'
                : '4px 0 24px rgba(0,0,0,0.1)'
              : 'none',
          transition: 'width 0.25s ease',
          overflow: 'hidden',
        },
      }}
    >
      {content}
    </Drawer>
  );
}
