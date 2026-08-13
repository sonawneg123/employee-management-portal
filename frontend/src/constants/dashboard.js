/**
 * @fileoverview Dashboard module constants.
 *
 * Centralises all magic values used across the dashboard: query keys,
 * refresh intervals, chart colours, activity icons, and leave status
 * labels. Import from this module exclusively to avoid duplication.
 */

import PeopleIcon from '@mui/icons-material/People';
import ApartmentIcon from '@mui/icons-material/Apartment';
import EventNoteIcon from '@mui/icons-material/EventNote';
import AccessTimeIcon from '@mui/icons-material/AccessTime';
import TrendingUpIcon from '@mui/icons-material/TrendingUp';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import PendingActionsIcon from '@mui/icons-material/PendingActions';
import CancelIcon from '@mui/icons-material/Cancel';

// ── React Query cache keys ────────────────────────────────────────────────────

/**
 * React Query key factory for all dashboard queries.
 * Using factory functions ensures consistent cache key shapes and
 * enables fine-grained invalidation.
 *
 * @readonly
 */
export const DASHBOARD_QUERY_KEYS = /** @type {const} */ ({
  all: () => ['dashboard'],
  summary: () => ['dashboard', 'summary'],
  activity: () => ['dashboard', 'activity'],
  charts: () => ['dashboard', 'charts'],
});

/**
 * Auto-refresh interval for dashboard data (5 minutes in milliseconds).
 *
 * @type {number}
 */
export const DASHBOARD_REFRESH_INTERVAL_MS = 5 * 60 * 1000;

// ── Chart colour palette ──────────────────────────────────────────────────────

/**
 * Colour palette for Recharts components.
 * Values are shared across all charts for visual consistency.
 * Each sub-array defines [light-mode colour, dark-mode colour].
 *
 * @type {string[]}
 */
export const CHART_COLORS = [
  '#4F46E5',
  '#7C3AED',
  '#10B981',
  '#F59E0B',
  '#3B82F6',
  '#EF4444',
  '#06B6D4',
  '#8B5CF6',
];

/**
 * Employee status colours mapped to EmployeeStatus enum values.
 *
 * @type {Record<string, string>}
 */
export const EMPLOYEE_STATUS_COLORS = {
  ACTIVE: '#10B981',
  INACTIVE: '#F59E0B',
  ON_LEAVE: '#3B82F6',
  TERMINATED: '#EF4444',
};

/**
 * Leave status colours mapped to LeaveStatus enum values.
 *
 * @type {Record<string, string>}
 */
export const LEAVE_STATUS_COLORS = {
  PENDING: '#F59E0B',
  APPROVED: '#10B981',
  REJECTED: '#EF4444',
  CANCELLED: '#94A3B8',
};

// ── Activity type metadata ────────────────────────────────────────────────────

/**
 * @typedef {Object} ActivityTypeMeta
 * @property {string}      color - MUI colour string for the activity icon chip.
 * @property {JSX.Element} icon  - MUI icon element.
 */

/**
 * Visual metadata for each dashboard activity type.
 *
 * @type {Record<string, ActivityTypeMeta>}
 */
export const ACTIVITY_TYPE_META = {
  EMPLOYEE_JOINED: { color: 'success', icon: PeopleIcon },
  LEAVE_SUBMITTED: { color: 'warning', icon: EventNoteIcon },
  LEAVE_APPROVED: { color: 'success', icon: CheckCircleIcon },
  LEAVE_REJECTED: { color: 'error', icon: CancelIcon },
  ATTENDANCE_MARKED: { color: 'info', icon: AccessTimeIcon },
  REVIEW_COMPLETED: { color: 'primary', icon: TrendingUpIcon },
  LEAVE_PENDING: { color: 'warning', icon: PendingActionsIcon },
};

// ── Stat card metadata ────────────────────────────────────────────────────────

/**
 * @typedef {Object} StatCardMeta
 * @property {string} label     - Display label.
 * @property {string} color     - MUI colour token for the icon background.
 * @property {string} iconColor - Explicit hex/CSS colour for the icon itself.
 */

/**
 * Metadata for the four main summary stat cards.
 *
 * @type {Record<string, StatCardMeta>}
 */
export const STAT_CARD_META = {
  totalEmployees: {
    label: 'Total Employees',
    color: 'rgba(79,70,229,0.1)',
    iconColor: '#4F46E5',
    Icon: PeopleIcon,
  },
  totalDepartments: {
    label: 'Departments',
    color: 'rgba(124,58,237,0.1)',
    iconColor: '#7C3AED',
    Icon: ApartmentIcon,
  },
  pendingLeaves: {
    label: 'Pending Leaves',
    color: 'rgba(245,158,11,0.12)',
    iconColor: '#F59E0B',
    Icon: EventNoteIcon,
  },
  presentToday: {
    label: 'Present Today',
    color: 'rgba(16,185,129,0.1)',
    iconColor: '#10B981',
    Icon: AccessTimeIcon,
  },
};

// ── Quick action definitions ──────────────────────────────────────────────────

/**
 * @typedef {Object} QuickAction
 * @property {string} id      - Unique identifier.
 * @property {string} label   - Display label.
 * @property {string} path    - Navigation route.
 * @property {string} color   - MUI colour token for the button.
 * @property {string[]} roles - Roles that can see this action.
 */
