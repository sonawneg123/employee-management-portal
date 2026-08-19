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
  LOGIN: '/login',
  REGISTER: '/register',
  FORGOT_PASSWORD: '/forgot-password',
  LOGIN_ADMIN: '/login/admin',
  LOGIN_HR: '/login/hr',
  LOGIN_EMPLOYEE: '/login/employee',
  REGISTER_HR: '/register/hr',
  REGISTER_EMPLOYEE: '/register/employee',

  // ── Protected (legacy — kept for backward compatibility) ────────
  DASHBOARD: '/dashboard',
  EMPLOYEES: '/employees',
  EMPLOYEE_DETAIL: (id) => `/employees/${id}`,
  DEPARTMENTS: '/departments',
  DEPARTMENT_DETAIL: (id) => `/departments/${id}`,
  LEAVES: '/leaves',
  LEAVE_DETAIL: (id) => `/leaves/${id}`,
  MY_LEAVES: '/leaves/my',
  ATTENDANCE: '/attendance',
  REVIEWS: '/reviews',
  PROFILE: '/profile',
  SETTINGS: '/settings',

  // ── Admin routes ─────────────────────────────────────────────────
  ADMIN_DASHBOARD: '/admin/dashboard',
  ADMIN_EMPLOYEES: '/admin/employees',
  ADMIN_EMPLOYEE_DETAIL: (id) => `/admin/employees/${id}`,
  ADMIN_DEPARTMENTS: '/admin/departments',
  ADMIN_DEPARTMENT_DETAIL: (id) => `/admin/departments/${id}`,
  ADMIN_LEAVES: '/admin/leaves',
  ADMIN_ATTENDANCE: '/admin/attendance',
  ADMIN_REVIEWS: '/admin/reviews',
  ADMIN_USERS: '/admin/users',

  // ── HR routes ────────────────────────────────────────────────────
  HR_DASHBOARD: '/hr/dashboard',
  HR_EMPLOYEES: '/hr/employees',
  HR_EMPLOYEE_DETAIL: (id) => `/hr/employees/${id}`,
  HR_DEPARTMENTS: '/hr/departments',
  HR_DEPARTMENT_DETAIL: (id) => `/hr/departments/${id}`,
  HR_LEAVES: '/hr/leaves',
  HR_ATTENDANCE: '/hr/attendance',
  HR_REVIEWS: '/hr/reviews',

  // ── Employee routes ──────────────────────────────────────────────
  EMPLOYEE_DASHBOARD: '/employee/dashboard',
  EMPLOYEE_LEAVES: '/employee/leaves',
  EMPLOYEE_ATTENDANCE: '/employee/attendance',
  EMPLOYEE_PROFILE: '/employee/profile',
  EMPLOYEE_REVIEWS: '/employee/reviews',

  // ── AI Assistant ────────────────────────────────────────────────
  AI_ASSISTANT: '/ai/assistant',

  // ── Company Policies (Admin + HR) ───────────────────────────────
  ADMIN_POLICIES: '/admin/policies',
  HR_POLICIES: '/hr/policies',

  // ── Manager Leave Management ─────────────────────────────────────
  MANAGER_LEAVES: '/hr/leave-approvals',

  // ── Task Management ──────────────────────────────────────────────
  MANAGER_TASKS: '/hr/tasks',
  MANAGER_TASK_DETAIL: (id) => `/hr/tasks/${id}`,
  MANAGER_TASK_REVIEWS: '/hr/task-reviews',
  EMPLOYEE_TASKS: '/employee/tasks',
  EMPLOYEE_TASK_DETAIL: (id) => `/employee/tasks/${id}`,
  ADMIN_TASKS: '/admin/tasks',
  ADMIN_TASK_DETAIL: (id) => `/admin/tasks/${id}`,

  // ── Analytics Dashboard (Phase 8A) ──────────────────────────────────────
  ANALYTICS: '/analytics',
  ADMIN_ANALYTICS: '/admin/analytics',
  HR_ANALYTICS: '/hr/analytics',
  EMPLOYEE_ANALYTICS: '/employee/analytics',

  // ── Fallback ─────────────────────────────────────────────────────
  NOT_FOUND: '/404',
  ACCESS_DENIED: '/403',
});
