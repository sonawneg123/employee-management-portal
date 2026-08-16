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
  AUTH_LOGIN: '/auth/login',
  AUTH_FORGOT_PASSWORD: '/auth/forgot-password',
  AUTH_VERIFY_OTP: '/auth/verify-otp',
  AUTH_RESET_PASSWORD: '/auth/reset-password',

  // Employees
  EMPLOYEES: '/employees',
  EMPLOYEE_BY_ID: (id) => `/employees/${id}`,

  // Departments
  DEPARTMENTS: '/departments',
  DEPARTMENT_BY_ID: (id) => `/departments/${id}`,

  // Leave requests
  LEAVES: '/leaves',
  LEAVES_MY: '/leaves/my',
  LEAVE_BY_ID: (id) => `/leaves/${id}`,
  LEAVE_APPROVE: (id) => `/leaves/${id}/approve`,
  LEAVE_REJECT: (id) => `/leaves/${id}/reject`,

  // Attendance
  ATTENDANCE: '/attendance',
  ATTENDANCE_MY: '/attendance/my',
  ATTENDANCE_CHECKIN: '/attendance/checkin',
  ATTENDANCE_CHECKOUT: '/attendance/checkout',
  ATTENDANCE_BY_ID: (id) => `/attendance/${id}`,

  // Performance reviews
  REVIEWS: '/reviews',
  REVIEW_BY_ID: (id) => `/reviews/${id}`,

  // AI Assistant
  AI_CHAT: '/ai/chat',

  // RAG Knowledge Base (Company Policies)
  KNOWLEDGE_DOCUMENTS: '/ai/rag/documents',
  KNOWLEDGE_DOCUMENT_BY_ID: (id) => `/ai/rag/documents/${id}`,

  // Tasks
  TASKS: '/tasks',
  TASK_BY_ID: (id) => `/tasks/${id}`,
  TASKS_MY: '/tasks/my',
  TASKS_CREATED: '/tasks/created',
  TASK_STATUS: (id) => `/tasks/${id}/status`,

  // Task Comments
  TASK_COMMENTS: (taskId) => `/tasks/${taskId}/comments`,

  // Task Attachments
  TASK_ATTACHMENTS: (taskId) => `/tasks/${taskId}/attachments`,
  TASK_ATTACHMENT_DOWNLOAD: (taskId, attachmentId) => `/tasks/${taskId}/attachments/${attachmentId}/download`,
  TASK_ATTACHMENT_BY_ID: (taskId, attachmentId) => `/tasks/${taskId}/attachments/${attachmentId}`,

  // Task Activities
  TASK_ACTIVITIES: (taskId) => `/tasks/${taskId}/activities`,

  // Task Reassignment
  TASK_REASSIGN: (taskId) => `/tasks/${taskId}/reassign`,

  // Employee Availability & Workload
  TASK_EMPLOYEE_AVAILABILITY: '/tasks/employee-availability',
  TASK_DASHBOARD_STATS: '/tasks/dashboard-stats',
  TASK_WORKLOAD_SUMMARY: '/tasks/workload-summary',
  TASK_WORKLOAD: (employeeId) => `/tasks/workload/${employeeId}`,

  // Task Submissions
  TASK_SUBMISSIONS: (taskId) => `/tasks/${taskId}/submissions`,
  TASK_SUBMISSIONS_LATEST: (taskId) => `/tasks/${taskId}/submissions/latest`,
  TASK_SUBMISSION_RESUBMIT: (submissionId) => `/task-submissions/${submissionId}/resubmit`,
  TASK_SUBMISSION_APPROVE: (submissionId) => `/task-submissions/${submissionId}/approve`,
  TASK_SUBMISSION_REQUEST_CHANGES: (submissionId) => `/task-submissions/${submissionId}/request-changes`,
  TASK_SUBMISSION_ATTACHMENT: (submissionId) => `/task-submissions/${submissionId}/attachment`,

  // Notifications
  NOTIFICATIONS: '/notifications',
  NOTIFICATIONS_UNREAD_COUNT: '/notifications/unread-count',
  NOTIFICATION_READ: (id) => `/notifications/${id}/read`,
  NOTIFICATIONS_READ_ALL: '/notifications/read-all',
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
