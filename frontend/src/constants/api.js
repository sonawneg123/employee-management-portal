/**
 * @fileoverview Base API URL and endpoint path constants.
 *
 * All Axios service calls should import path segments from here rather
 * than hard-coding strings in the API layer files.
 */

/**
 * Base URL for all API requests.
 * During development Vite proxies `/api` to `http://localhost:8080/api`.
 * In production, Nginx rewrites `/api` to the backend container.
 *
 * @type {string}
 */
export const API_BASE_URL = '/api';

/**
 * Timeout in milliseconds for all HTTP requests.
 *
 * @type {number}
 */
export const REQUEST_TIMEOUT_MS = 15_000;

/**
 * Individual API endpoint paths (relative to {@link API_BASE_URL}).
 *
 * @readonly
 * @enum {string}
 */
export const API_ENDPOINTS = /** @type {const} */ ({
  // Auth
  AUTH_REGISTER: '/auth/register',
  AUTH_LOGIN:    '/auth/login',

  // Employees
  EMPLOYEES:     '/employees',
  EMPLOYEE_BY_ID: (id) => `/employees/${id}`,

  // Departments
  DEPARTMENTS:     '/departments',
  DEPARTMENT_BY_ID: (id) => `/departments/${id}`,

  // Leave requests
  LEAVES:        '/leaves',
  LEAVES_MY:     '/leaves/my',
  LEAVE_BY_ID:   (id) => `/leaves/${id}`,
  LEAVE_APPROVE: (id) => `/leaves/${id}/approve`,
  LEAVE_REJECT:  (id) => `/leaves/${id}/reject`,

  // Attendance
  ATTENDANCE:        '/attendance',
  ATTENDANCE_MY:     '/attendance/my',
  ATTENDANCE_BY_ID:  (id) => `/attendance/${id}`,

  // Performance reviews
  REVIEWS:     '/reviews',
  REVIEW_BY_ID: (id) => `/reviews/${id}`,
});

/**
 * Local-storage key used to persist the JWT access token.
 *
 * @type {string}
 */
export const TOKEN_STORAGE_KEY = 'emp_portal_token';

/**
 * Local-storage key used to persist the authenticated user object.
 *
 * @type {string}
 */
export const USER_STORAGE_KEY = 'emp_portal_user';
