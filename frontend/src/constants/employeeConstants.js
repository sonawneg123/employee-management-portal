/**
 * @fileoverview Employee module constants.
 *
 * Centralises all magic values used across the employee management module:
 * status options, sort fields, page sizes, query keys, and CSV headers.
 * Import exclusively from here to avoid scattered magic strings.
 */

// ── React Query cache keys ────────────────────────────────────────────────────

/**
 * React Query key factory for all employee queries.
 *
 * @readonly
 */
export const EMPLOYEE_QUERY_KEYS = /** @type {const} */ ({
  all: () => ['employees'],
  lists: () => ['employees', 'list'],
  list: (params) => ['employees', 'list', params],
  detail: (id) => ['employees', 'detail', id],
});

/**
 * React Query key factory for department queries used within the employee module.
 *
 * @readonly
 */
export const DEPARTMENT_QUERY_KEYS = /** @type {const} */ ({
  all: () => ['departments'],
  list: () => ['departments', 'list'],
});

// ── Employee status ───────────────────────────────────────────────────────────

/**
 * @typedef {Object} StatusOption
 * @property {string} value  - API enum value.
 * @property {string} label  - Human-readable label.
 * @property {string} color  - MUI Chip color prop value.
 */

/**
 * All valid employee status options.
 *
 * @type {StatusOption[]}
 */
export const EMPLOYEE_STATUS_OPTIONS = [
  { value: 'ACTIVE', label: 'Active', color: 'success' },
  { value: 'INACTIVE', label: 'Inactive', color: 'default' },
  { value: 'ON_LEAVE', label: 'On Leave', color: 'info' },
  { value: 'TERMINATED', label: 'Terminated', color: 'error' },
];

/**
 * Lookup map: status value → StatusOption.
 *
 * @type {Record<string, StatusOption>}
 */
export const EMPLOYEE_STATUS_MAP = Object.fromEntries(
  EMPLOYEE_STATUS_OPTIONS.map((o) => [o.value, o]),
);

// ── Sort fields ───────────────────────────────────────────────────────────────

/**
 * @typedef {Object} SortOption
 * @property {string} value  - API sort field name.
 * @property {string} label  - Human-readable label.
 */

/**
 * Valid sortable fields for the employee list API.
 *
 * @type {SortOption[]}
 */
export const EMPLOYEE_SORT_OPTIONS = [
  { value: 'createdAt', label: 'Date Added' },
  { value: 'employeeCode', label: 'Employee Code' },
  { value: 'jobTitle', label: 'Job Title' },
  { value: 'dateOfJoining', label: 'Join Date' },
  { value: 'salary', label: 'Salary' },
];

// ── Pagination ────────────────────────────────────────────────────────────────

/**
 * Available page size options for the employee table.
 *
 * @type {number[]}
 */
export const EMPLOYEE_PAGE_SIZE_OPTIONS = [10, 20, 50, 100];

/**
 * Default page size for the employee list.
 *
 * @type {number}
 */
export const EMPLOYEE_DEFAULT_PAGE_SIZE = 20;

/**
 * Default sort field.
 *
 * @type {string}
 */
export const EMPLOYEE_DEFAULT_SORT = 'createdAt';

/**
 * Default sort direction.
 *
 * @type {'asc' | 'desc'}
 */
export const EMPLOYEE_DEFAULT_DIRECTION = 'desc';

// ── Table columns ─────────────────────────────────────────────────────────────

/**
 * Column ID constants for the employee table (used for sort keys).
 *
 * @readonly
 * @enum {string}
 */
export const EMPLOYEE_COLUMNS = /** @type {const} */ ({
  EMPLOYEE_CODE: 'employeeCode',
  FULL_NAME: 'firstName',
  JOB_TITLE: 'jobTitle',
  DEPARTMENT: 'departmentName',
  STATUS: 'status',
  DATE_OF_JOINING: 'dateOfJoining',
  SALARY: 'salary',
  ACTIONS: 'actions',
});

// ── CSV Export ────────────────────────────────────────────────────────────────

/**
 * CSV column headers for the employee export.
 * The order here determines column order in the exported file.
 *
 * @type {string[]}
 */
export const CSV_HEADERS = [
  'Employee Code',
  'First Name',
  'Last Name',
  'Email',
  'Job Title',
  'Department',
  'Status',
  'Phone',
  'Date of Joining',
  'Salary',
];

/**
 * CSV field accessor keys matching CSV_HEADERS order.
 *
 * @type {string[]}
 */
export const CSV_FIELDS = [
  'employeeCode',
  'firstName',
  'lastName',
  'email',
  'jobTitle',
  'departmentName',
  'status',
  'phone',
  'dateOfJoining',
  'salary',
];

// ── Debounce ──────────────────────────────────────────────────────────────────

/**
 * Debounce delay (ms) for the search input before triggering an API call.
 *
 * @type {number}
 */
export const SEARCH_DEBOUNCE_MS = 400;

// ── Form defaults ─────────────────────────────────────────────────────────────

/**
 * Default values for the create employee form.
 *
 * @type {Object}
 */
export const EMPLOYEE_FORM_DEFAULTS = {
  employeeCode: '',
  firstName: '',
  lastName: '',
  email: '',
  jobTitle: '',
  departmentId: '',
  phone: '',
  address: '',
  dateOfJoining: '',
  salary: '',
  status: 'ACTIVE',
  managerId: '',
  profilePhotoUrl: '',
};
