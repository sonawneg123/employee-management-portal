/**
 * @fileoverview Sidebar — premium SaaS navigation drawer for PeopleCore HR.
 *
 * Deep navy sidebar with pill-shaped active items and gold accent.
 * Inspired by premium SaaS HR product navigation.
 * Role-aware — each role sees only its own routes.
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
import AutoAwesomeRoundedIcon from '@mui/icons-material/AutoAwesomeRounded';
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
    label: 'AI Copilot',
    path: ROUTES.AI_ASSISTANT,
    icon: <AutoAwesomeRoundedIcon />,
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

  // Premium SaaS nav colours — deep navy sidebar
  const bg = isDark ? '#0C1220' : '#1A2342';
  const activeBg = isDark ? 'rgba(245,197,24,0.15)' : 'rgba(245,197,24,0.15)';
  const activeColor = '#F5C518';
  const hoverBg = isDark ? 'rgba(255,255,255,0.07)' : 'rgba(255,255,255,0.08)';
  const textColor = isDark ? 'rgba(240,237,230,0.75)' : 'rgba(255,255,255,0.75)';
  const sectionLabelColor = isDark ? 'rgba(240,237,230,0.35)' : 'rgba(255,255,255,0.35)';
  const borderColor = isDark ? 'rgba(240,237,230,0.07)' : 'rgba(255,255,255,0.1)';

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

    const isAiItem = item.path === ROUTES.AI_ASSISTANT;

    const btn = (
      <ListItemButton
        component={NavLink}
        to={item.path}
        onClick={variant === 'temporary' ? onClose : undefined}
        selected={isActive}
        sx={{
          borderRadius: '12px',
          mx: 1,
          px: collapsed ? 1.25 : 1.5,
          py: 1,
          minHeight: 40,
          justifyContent: collapsed ? 'center' : 'flex-start',
          bgcolor: isActive ? activeBg : 'transparent',
          color: isActive ? activeColor : isAiItem ? 'rgba(245,197,24,0.6)' : textColor,
          border: isActive ? `1px solid rgba(245,197,24,0.25)` : '1px solid transparent',
          '&:hover': {
            bgcolor: isActive ? activeBg : hoverBg,
            color: isActive ? activeColor : '#FFFFFF',
            border: isActive
              ? '1px solid rgba(245,197,24,0.25)'
              : '1px solid rgba(255,255,255,0.1)',
          },
          '&.Mui-selected': {
            bgcolor: activeBg,
            '&:hover': { bgcolor: activeBg },
          },
          transition: 'all 0.16s ease',
        }}
        aria-label={item.label}
        aria-current={isActive ? 'page' : undefined}
      >
        <ListItemIcon
          sx={{
            minWidth: collapsed ? 0 : 34,
            color: 'inherit',
            justifyContent: 'center',
            '& svg': { fontSize: 18 },
          }}
        >
          {item.icon}
        </ListItemIcon>
        {!collapsed && (
          <ListItemText
            primary={item.label}
            primaryTypographyProps={{
              fontSize: '0.85rem',
              fontWeight: isActive ? 600 : 500,
              noWrap: true,
              letterSpacing: '0.005em',
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
              bgcolor: isCheckedOut ? '#10B981' : '#F59E0B',
              flexShrink: 0,
              mr: 0.5,
              boxShadow: isCheckedOut
                ? '0 0 6px rgba(16,185,129,0.6)'
                : '0 0 6px rgba(245,158,11,0.6)',
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
        overflow: 'hidden',
        transition: 'width 0.28s cubic-bezier(0.4,0,0.2,1)',
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
          borderBottom: `1px solid ${borderColor}`,
          justifyContent: collapsed ? 'center' : 'flex-start',
          transition: 'padding 0.28s ease',
          minHeight: 68,
        }}
      >
        {/* Logo mark */}
        <Box
          sx={{
            width: 34,
            height: 34,
            borderRadius: '10px',
            background: 'linear-gradient(135deg, #F5C518, #C49A00)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            flexShrink: 0,
            boxShadow: '0 4px 12px rgba(245,197,24,0.4)',
          }}
          aria-hidden="true"
        >
          <PeopleAltIcon sx={{ color: '#1A2342', fontSize: 18 }} />
        </Box>
        {!collapsed && (
          <Box sx={{ minWidth: 0 }}>
            <Typography
              variant="body2"
              fontWeight={800}
              noWrap
              sx={{
                color: '#FFFFFF',
                letterSpacing: '-0.02em',
                lineHeight: 1.2,
                fontSize: '0.9375rem',
              }}
            >
              PeopleCore HR
            </Typography>
            <Typography
              variant="caption"
              sx={{ color: 'rgba(255,255,255,0.45)', fontSize: '0.675rem' }}
            >
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
              fontSize: '0.6rem',
              letterSpacing: '0.12em',
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
                  fontSize: '0.6rem',
                  letterSpacing: '0.12em',
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
              fontSize: '0.6rem',
              letterSpacing: '0.12em',
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
          borderTop: `1px solid ${borderColor}`,
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
                background: 'linear-gradient(135deg, #2D3A6B, #4F6AB5)',
                fontSize: '0.75rem',
                fontWeight: 700,
                flexShrink: 0,
                border: '2px solid rgba(245,197,24,0.3)',
              }}
            >
              {!user.profilePhotoUrl && initials}
            </Avatar>
            <Box sx={{ minWidth: 0 }}>
              <Typography
                variant="caption"
                fontWeight={700}
                noWrap
                sx={{ display: 'block', color: '#FFFFFF', lineHeight: 1.3 }}
              >
                {user.firstName} {user.lastName}
              </Typography>
              <Typography
                variant="caption"
                noWrap
                sx={{ color: 'rgba(255,255,255,0.45)', fontSize: '0.65rem', display: 'block' }}
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
                bgcolor: 'rgba(255,255,255,0.08)',
                color: 'rgba(255,255,255,0.5)',
                flexShrink: 0,
                transition: 'all 0.15s ease',
                '&:hover': {
                  bgcolor: 'rgba(245,197,24,0.15)',
                  color: '#F5C518',
                },
                '&:focus-visible': { outline: `2px solid #F5C518`, outlineOffset: 2 },
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
          bgcolor: bg,
          boxShadow:
            variant === 'temporary'
              ? isDark
                ? '4px 0 32px rgba(0,0,0,0.7)'
                : '4px 0 32px rgba(26,35,66,0.5)'
              : '2px 0 16px rgba(0,0,0,0.15)',
          transition: 'width 0.28s cubic-bezier(0.4,0,0.2,1)',
          overflow: 'hidden',
        },
      }}
    >
      {content}
    </Drawer>
  );
}
