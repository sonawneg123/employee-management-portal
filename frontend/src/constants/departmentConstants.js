/**
 * @fileoverview Department module constants.
 *
 * Centralises all magic values used across the department management module:
 * React Query keys, sort fields, page sizes, CSV headers, and form defaults.
 */

// ── React Query cache keys ────────────────────────────────────────────────────

/**
 * React Query key factory for all department module queries.
 * These keys are intentionally separate from the shared DEPARTMENT_QUERY_KEYS
 * in employeeConstants.js which is used for the simple flat-list dropdown.
 *
 * @readonly
 */
export const DEPT_QUERY_KEYS = /** @type {const} */ ({
  all:    ()       => ['dept-mgmt'],
  lists:  ()       => ['dept-mgmt', 'list'],
  list:   (params) => ['dept-mgmt', 'list', params],
  detail: (id)     => ['dept-mgmt', 'detail', id],
});

// ── Sort fields ───────────────────────────────────────────────────────────────

/**
 * @typedef {Object} SortOption
 * @property {string} value - API sort field name.
 * @property {string} label - Human-readable label.
 */

/**
 * Valid sortable fields for the department list API.
 *
 * @type {SortOption[]}
 */
export const DEPARTMENT_SORT_OPTIONS = [
  { value: 'name',      label: 'Name'        },
  { value: 'code',      label: 'Code'        },
  { value: 'createdAt', label: 'Date Created' },
];

// ── Pagination ────────────────────────────────────────────────────────────────

/**
 * Available page-size options.
 *
 * @type {number[]}
 */
export const DEPARTMENT_PAGE_SIZE_OPTIONS = [10, 20, 50];

/**
 * Default page size.
 *
 * @type {number}
 */
export const DEPARTMENT_DEFAULT_PAGE_SIZE = 20;

/**
 * Default sort field.
 *
 * @type {string}
 */
export const DEPARTMENT_DEFAULT_SORT = 'name';

/**
 * Default sort direction.
 *
 * @type {'asc'|'desc'}
 */
export const DEPARTMENT_DEFAULT_DIRECTION = 'asc';

// ── Table column IDs ──────────────────────────────────────────────────────────

/**
 * Column ID constants for the department table (used as sort keys).
 *
 * @readonly
 * @enum {string}
 */
export const DEPARTMENT_COLUMNS = /** @type {const} */ ({
  AVATAR:         'avatar',
  NAME:           'name',
  CODE:           'code',
  EMPLOYEE_COUNT: 'employeeCount',
  HEAD:           'headName',
  CREATED_AT:     'createdAt',
  ACTIONS:        'actions',
});

// ── CSV Export ────────────────────────────────────────────────────────────────

/**
 * CSV column headers for the department export.
 *
 * @type {string[]}
 */
export const DEPT_CSV_HEADERS = [
  'Name',
  'Code',
  'Description',
  'Department Head',
  'Employee Count',
  'Created At',
];

/**
 * CSV field accessor keys matching DEPT_CSV_HEADERS order.
 *
 * @type {string[]}
 */
export const DEPT_CSV_FIELDS = [
  'name',
  'code',
  'description',
  'headName',
  'employeeCount',
  'createdAt',
];

// ── Search debounce ───────────────────────────────────────────────────────────

/**
 * Debounce delay (ms) for the department search input.
 *
 * @type {number}
 */
export const DEPT_SEARCH_DEBOUNCE_MS = 400;

// ── Avatar colours ────────────────────────────────────────────────────────────

/**
 * Eight deterministic avatar background colours for department avatars.
 * Colour is chosen based on the first character of the department name.
 *
 * @type {string[]}
 */
export const DEPT_AVATAR_COLORS = [
  '#1976d2', '#7c3aed', '#2e7d32', '#ed6c02',
  '#0288d1', '#c62828', '#00796b', '#f57f17',
];

// ── Form defaults ─────────────────────────────────────────────────────────────

/**
 * Default form values for the create department form.
 *
 * @type {Object}
 */
export const DEPARTMENT_FORM_DEFAULTS = {
  name:        '',
  code:        '',
  description: '',
  headName:    '',
};
