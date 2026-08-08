/**
 * @fileoverview Application route path constants.
 *
 * Centralising route strings here prevents magic strings scattered across
 * the codebase. Import this module wherever a navigation path is needed.
 */

/**
 * All named route paths for the application.
 *
 * @readonly
 * @enum {string}
 */
export const ROUTES = /** @type {const} */ ({
  // ── Public ──────────────────────────────────────────────────────
  LOGIN:    '/login',
  REGISTER: '/register',

  // ── Protected ───────────────────────────────────────────────────
  DASHBOARD:       '/dashboard',
  EMPLOYEES:       '/employees',
  EMPLOYEE_DETAIL:   (id) => `/employees/${id}`,
  DEPARTMENTS:       '/departments',
  DEPARTMENT_DETAIL: (id) => `/departments/${id}`,
  LEAVES:          '/leaves',
  LEAVE_DETAIL:    (id) => `/leaves/${id}`,
  MY_LEAVES:       '/leaves/my',
  ATTENDANCE:      '/attendance',
  REVIEWS:         '/reviews',
  PROFILE:         '/profile',
  SETTINGS:        '/settings',

  // ── Fallback ─────────────────────────────────────────────────────
  NOT_FOUND:     '/404',
  ACCESS_DENIED: '/403',
});
