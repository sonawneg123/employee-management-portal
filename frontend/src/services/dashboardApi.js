/**
 * @fileoverview Dashboard API service.
 *
 * Provides typed wrappers for the three dashboard aggregation endpoints.
 * All requests flow through the shared Axios instance so that authentication
 * and error normalisation are applied automatically.
 *
 * Backend endpoints (to be implemented in Phase 4 backend extension):
 *   GET /api/dashboard/summary  — KPI counters
 *   GET /api/dashboard/activity — recent activity feed
 *   GET /api/dashboard/charts   — chart datasets
 */

import axiosInstance from '@/api/axiosInstance';

// ── Endpoint paths ────────────────────────────────────────────────────────────

const BASE = '/dashboard';

const ENDPOINTS = {
  SUMMARY:  `${BASE}/summary`,
  ACTIVITY: `${BASE}/activity`,
  CHARTS:   `${BASE}/charts`,
};

// ── Type definitions ──────────────────────────────────────────────────────────

/**
 * @typedef {Object} DashboardSummary
 * @property {number} totalEmployees    - Total headcount.
 * @property {number} totalDepartments  - Number of departments.
 * @property {number} pendingLeaves     - Leave requests awaiting approval.
 * @property {number} presentToday      - Employees marked present today.
 * @property {number} activeEmployees   - Employees with ACTIVE status.
 * @property {number} onLeaveToday      - Employees on approved leave today.
 * @property {number} newThisMonth      - Employees who joined this month.
 * @property {number} trendEmployees    - Month-over-month headcount change.
 * @property {number} trendLeaves       - Change in pending leaves vs last week.
 * @property {number} trendAttendance   - Attendance rate change vs yesterday.
 * @property {number} attendanceRate    - Today's attendance rate (0–1).
 */

/**
 * @typedef {Object} ActivityItem
 * @property {string}  id          - Unique activity ID.
 * @property {string}  type        - Activity type key (see ACTIVITY_TYPE_META).
 * @property {string}  description - Human-readable description.
 * @property {string}  timestamp   - ISO-8601 timestamp.
 * @property {string}  [actorName] - Name of the user who triggered the event.
 * @property {string}  [targetName]- Name of the affected entity.
 */

/**
 * @typedef {Object} DepartmentDistribution
 * @property {string} name  - Department name.
 * @property {number} count - Employee headcount.
 * @property {string} code  - Department code.
 */

/**
 * @typedef {Object} AttendanceTrendPoint
 * @property {string} date    - "YYYY-MM-DD" date string.
 * @property {number} present - Present count.
 * @property {number} absent  - Absent count.
 */

/**
 * @typedef {Object} DashboardCharts
 * @property {DepartmentDistribution[]}  departmentDistribution - Data for the department pie chart.
 * @property {AttendanceTrendPoint[]}    attendanceTrend        - Last-14-days attendance line chart data.
 * @property {Array<{status: string, count: number}>} employeeStatusBreakdown - Employee status bar chart data.
 */

// ── API functions ─────────────────────────────────────────────────────────────

/**
 * Fetches the dashboard KPI summary.
 *
 * @returns {Promise<DashboardSummary>}
 */
export async function getDashboardSummary() {
  const { data } = await axiosInstance.get(ENDPOINTS.SUMMARY);
  return data;
}

/**
 * Fetches the recent activity feed.
 *
 * @param {{ limit?: number }} [params={}]
 * @returns {Promise<ActivityItem[]>}
 */
export async function getDashboardActivity(params = {}) {
  const { data } = await axiosInstance.get(ENDPOINTS.ACTIVITY, { params });
  return data;
}

/**
 * Fetches all chart datasets in a single request.
 *
 * @returns {Promise<DashboardCharts>}
 */
export async function getDashboardCharts() {
  const { data } = await axiosInstance.get(ENDPOINTS.CHARTS);
  return data;
}
