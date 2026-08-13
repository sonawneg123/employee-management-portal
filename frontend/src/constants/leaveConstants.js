/**
 * @fileoverview Leave management module constants.
 *
 * Centralises all magic values: React Query keys, leave types, statuses,
 * sort options, page sizes, CSV config, and form defaults.
 */

// ── React Query keys ──────────────────────────────────────────────────────────

/**
 * React Query key factory for all leave module queries.
 *
 * @readonly
 */
export const LEAVE_QUERY_KEYS = /** @type {const} */ ({
  all: () => ['leaves'],
  lists: () => ['leaves', 'list'],
  list: (params) => ['leaves', 'list', params],
  detail: (id) => ['leaves', 'detail', id],
  my: (params) => ['leaves', 'my', params],
});

// ── Leave types ───────────────────────────────────────────────────────────────

/**
 * @typedef {Object} LeaveTypeOption
 * @property {string} value  - API enum value.
 * @property {string} label  - Human-readable label.
 * @property {string} color  - MUI Chip color prop.
 * @property {string} icon   - Emoji icon for quick recognition.
 */

/**
 * All valid leave type options.
 *
 * @type {LeaveTypeOption[]}
 */
export const LEAVE_TYPE_OPTIONS = [
  { value: 'ANNUAL', label: 'Annual Leave', color: 'primary', icon: '🏖️' },
  { value: 'SICK', label: 'Sick Leave', color: 'error', icon: '🤒' },
  { value: 'MATERNITY', label: 'Maternity Leave', color: 'secondary', icon: '🤱' },
  { value: 'PATERNITY', label: 'Paternity Leave', color: 'info', icon: '👨‍👦' },
  { value: 'UNPAID', label: 'Unpaid Leave', color: 'warning', icon: '💰' },
  { value: 'EMERGENCY', label: 'Emergency Leave', color: 'error', icon: '🚨' },
  { value: 'STUDY', label: 'Study Leave', color: 'success', icon: '📚' },
  { value: 'OTHER', label: 'Other', color: 'default', icon: '📋' },
];

/**
 * Lookup map: leaveType → LeaveTypeOption.
 *
 * @type {Record<string, LeaveTypeOption>}
 */
export const LEAVE_TYPE_MAP = Object.fromEntries(LEAVE_TYPE_OPTIONS.map((o) => [o.value, o]));

// ── Leave statuses ────────────────────────────────────────────────────────────

/**
 * @typedef {Object} LeaveStatusOption
 * @property {string} value
 * @property {string} label
 * @property {string} color  - MUI Chip color.
 */

/**
 * All valid leave status options.
 *
 * @type {LeaveStatusOption[]}
 */
export const LEAVE_STATUS_OPTIONS = [
  { value: 'PENDING', label: 'Pending', color: 'warning' },
  { value: 'APPROVED', label: 'Approved', color: 'success' },
  { value: 'REJECTED', label: 'Rejected', color: 'error' },
  { value: 'CANCELLED', label: 'Cancelled', color: 'default' },
];

/**
 * Lookup map: status → LeaveStatusOption.
 *
 * @type {Record<string, LeaveStatusOption>}
 */
export const LEAVE_STATUS_MAP = Object.fromEntries(LEAVE_STATUS_OPTIONS.map((o) => [o.value, o]));

// ── Sort options ──────────────────────────────────────────────────────────────

/**
 * Valid sort fields for the leave list.
 *
 * @type {Array<{value: string, label: string}>}
 */
export const LEAVE_SORT_OPTIONS = [
  { value: 'createdAt', label: 'Date Submitted' },
  { value: 'startDate', label: 'Start Date' },
  { value: 'endDate', label: 'End Date' },
  { value: 'leaveType', label: 'Leave Type' },
  { value: 'status', label: 'Status' },
];

// ── Pagination ────────────────────────────────────────────────────────────────

/** @type {number[]} */
export const LEAVE_PAGE_SIZE_OPTIONS = [10, 20, 50];

/** @type {number} */
export const LEAVE_DEFAULT_PAGE_SIZE = 20;

/** @type {string} */
export const LEAVE_DEFAULT_SORT = 'createdAt';

/** @type {'asc'|'desc'} */
export const LEAVE_DEFAULT_DIRECTION = 'desc';

// ── Table column IDs ──────────────────────────────────────────────────────────

/**
 * @readonly
 * @enum {string}
 */
export const LEAVE_COLUMNS = /** @type {const} */ ({
  EMPLOYEE: 'employeeName',
  TYPE: 'leaveType',
  START_DATE: 'startDate',
  END_DATE: 'endDate',
  DAYS: 'totalDays',
  STATUS: 'status',
  SUBMITTED: 'createdAt',
  ACTIONS: 'actions',
});

// ── CSV Export ────────────────────────────────────────────────────────────────

/** @type {string[]} */
export const LEAVE_CSV_HEADERS = [
  'Employee',
  'Code',
  'Department',
  'Leave Type',
  'Start Date',
  'End Date',
  'Total Days',
  'Status',
  'Reason',
  'Submitted',
];

/** @type {string[]} */
export const LEAVE_CSV_FIELDS = [
  'employeeName',
  'employeeCode',
  'departmentName',
  'leaveType',
  'startDate',
  'endDate',
  'totalDays',
  'status',
  'reason',
  'createdAt',
];

// ── Search debounce ───────────────────────────────────────────────────────────

/** @type {number} */
export const LEAVE_SEARCH_DEBOUNCE_MS = 400;

// ── Leave balance (illustrative defaults — backend drives real values) ────────

/**
 * Default annual leave entitlements per type (in working days).
 * These are UI-side defaults — the backend is the source of truth.
 *
 * @type {Record<string, number>}
 */
export const LEAVE_DEFAULT_ENTITLEMENT = {
  ANNUAL: 20,
  SICK: 10,
  MATERNITY: 90,
  PATERNITY: 14,
  UNPAID: 999,
  EMERGENCY: 3,
  STUDY: 5,
  OTHER: 5,
};

// ── Calendar colours ──────────────────────────────────────────────────────────

/**
 * Background colours for calendar event chips (one per leave type).
 *
 * @type {Record<string, string>}
 */
export const LEAVE_CALENDAR_COLORS = {
  ANNUAL: '#1976d2',
  SICK: '#c62828',
  MATERNITY: '#7c3aed',
  PATERNITY: '#0288d1',
  UNPAID: '#f57f17',
  EMERGENCY: '#b71c1c',
  STUDY: '#2e7d32',
  OTHER: '#757575',
};

// ── Form defaults ─────────────────────────────────────────────────────────────

/**
 * @type {Object}
 */
export const LEAVE_FORM_DEFAULTS = {
  leaveType: 'ANNUAL',
  startDate: '',
  endDate: '',
  reason: '',
  isEmergency: false,
  attachmentUrl: '',
};
