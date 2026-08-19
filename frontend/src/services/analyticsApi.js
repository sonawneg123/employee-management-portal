/**
 * @fileoverview Analytics API service — Phase 8A.
 *
 * Wraps the six analytics REST endpoints:
 *   GET /api/analytics/summary
 *   GET /api/analytics/attendance
 *   GET /api/analytics/leaves
 *   GET /api/analytics/tasks
 *   GET /api/analytics/performance
 *   GET /api/analytics/departments
 *
 * All functions accept an optional {@link AnalyticsFilters} object that is
 * serialised to query parameters.
 */

import axiosInstance from '@/api/axiosInstance';

// ── Base path ─────────────────────────────────────────────────────────────────

const BASE = '/analytics';

// ── Type definitions ──────────────────────────────────────────────────────────

/**
 * @typedef {Object} AnalyticsFilters
 * @property {string}  [from]         - Start date YYYY-MM-DD
 * @property {string}  [to]           - End date YYYY-MM-DD
 * @property {string}  [departmentId] - UUID
 * @property {string}  [employeeId]   - UUID
 */

/**
 * @typedef {Object} TrendPoint
 * @property {string} label
 * @property {number} value
 */

/**
 * @typedef {Object} AnalyticsSummary
 * @property {number} totalEmployees
 * @property {number} activeEmployees
 * @property {number} inactiveEmployees
 * @property {number} employeesOnLeave
 * @property {number} newEmployees
 * @property {number} attendanceRate
 * @property {number} presentCount
 * @property {number} absentCount
 * @property {number} halfDayCount
 * @property {number} onLeaveCount
 * @property {number} totalLeaveRequests
 * @property {number} pendingLeaveRequests
 * @property {number} approvedLeaveRequests
 * @property {number} rejectedLeaveRequests
 * @property {number} totalTasks
 * @property {number} completedTasks
 * @property {number} pendingTasks
 * @property {number} overdueTasks
 * @property {number} taskCompletionRate
 * @property {number} avgAiScore
 * @property {number} completedAiEvaluations
 * @property {number} failedAiEvaluations
 * @property {TrendPoint[]} attendanceTrend
 * @property {TrendPoint[]} aiScoreTrend
 */

/**
 * @typedef {Object} DailyAttendancePoint
 * @property {string} date
 * @property {number} present
 * @property {number} absent
 * @property {number} total
 * @property {number} rate
 */

/**
 * @typedef {Object} AnalyticsAttendance
 * @property {number}                totalRecords
 * @property {number}                presentCount
 * @property {number}                absentCount
 * @property {number}                halfDayCount
 * @property {number}                workFromHomeCount
 * @property {number}                onLeaveCount
 * @property {number}                attendanceRate
 * @property {DailyAttendancePoint[]} trend
 */

/**
 * @typedef {Object} LeaveTypeBreakdown
 * @property {string} leaveType
 * @property {number} count
 */

/**
 * @typedef {Object} MonthlyLeaveTrend
 * @property {string} month
 * @property {number} total
 * @property {number} approved
 */

/**
 * @typedef {Object} AnalyticsLeaves
 * @property {number}               totalRequests
 * @property {number}               pendingCount
 * @property {number}               approvedCount
 * @property {number}               rejectedCount
 * @property {number}               cancelledCount
 * @property {number}               leaveUtilizationRate
 * @property {LeaveTypeBreakdown[]} byType
 * @property {MonthlyLeaveTrend[]}  trend
 */

/**
 * @typedef {Object} TaskStatusBreakdown
 * @property {string} status
 * @property {number} count
 */

/**
 * @typedef {Object} AnalyticsTasks
 * @property {number}               totalTasks
 * @property {number}               completedTasks
 * @property {number}               assignedTasks
 * @property {number}               inProgressTasks
 * @property {number}               submittedTasks
 * @property {number}               overdueTasks
 * @property {number}               draftTasks
 * @property {number}               completionRate
 * @property {TaskStatusBreakdown[]} statusBreakdown
 */

/**
 * @typedef {Object} ScoreTrendPoint
 * @property {string} label
 * @property {number} avgScore
 * @property {number} evaluationCount
 */

/**
 * @typedef {Object} AnalyticsPerformance
 * @property {number}           avgCompletionScore
 * @property {number}           avgQualityScore
 * @property {number}           completedEvaluations
 * @property {number}           failedEvaluations
 * @property {number}           pendingEvaluations
 * @property {ScoreTrendPoint[]} scoreTrend
 */

/**
 * @typedef {Object} DepartmentStat
 * @property {string} departmentId
 * @property {string} departmentName
 * @property {string} departmentCode
 * @property {number} headcount
 * @property {number} activeCount
 * @property {number} onLeaveCount
 */

/**
 * @typedef {Object} AnalyticsDepartments
 * @property {number}          totalDepartments
 * @property {DepartmentStat[]} departments
 */

// ── Helper ────────────────────────────────────────────────────────────────────

/**
 * Strips undefined/null values from a filter params object.
 *
 * @param {AnalyticsFilters} filters
 * @returns {Record<string, string>}
 */
function buildParams(filters = {}) {
  const params = {};
  if (filters.from) params.from = filters.from;
  if (filters.to) params.to = filters.to;
  if (filters.departmentId) params.departmentId = filters.departmentId;
  if (filters.employeeId) params.employeeId = filters.employeeId;
  return params;
}

// ── API functions ─────────────────────────────────────────────────────────────

/**
 * Fetches the analytics summary KPIs.
 *
 * @param {AnalyticsFilters} [filters={}]
 * @returns {Promise<AnalyticsSummary>}
 */
export async function getAnalyticsSummary(filters = {}) {
  const { data } = await axiosInstance.get(`${BASE}/summary`, {
    params: buildParams(filters),
  });
  return data;
}

/**
 * Fetches attendance analytics with daily trend.
 *
 * @param {AnalyticsFilters} [filters={}]
 * @returns {Promise<AnalyticsAttendance>}
 */
export async function getAnalyticsAttendance(filters = {}) {
  const { data } = await axiosInstance.get(`${BASE}/attendance`, {
    params: buildParams(filters),
  });
  return data;
}

/**
 * Fetches leave analytics with type breakdown and monthly trend.
 *
 * @param {AnalyticsFilters} [filters={}]
 * @returns {Promise<AnalyticsLeaves>}
 */
export async function getAnalyticsLeaves(filters = {}) {
  const { data } = await axiosInstance.get(`${BASE}/leaves`, {
    params: buildParams(filters),
  });
  return data;
}

/**
 * Fetches task analytics with status breakdown.
 *
 * @param {AnalyticsFilters} [filters={}]
 * @returns {Promise<AnalyticsTasks>}
 */
export async function getAnalyticsTasks(filters = {}) {
  const { data } = await axiosInstance.get(`${BASE}/tasks`, {
    params: buildParams(filters),
  });
  return data;
}

/**
 * Fetches AI performance analytics.
 * ADMIN / HR / MANAGER only.
 *
 * @param {Pick<AnalyticsFilters,'from'|'to'>} [filters={}]
 * @returns {Promise<AnalyticsPerformance>}
 */
export async function getAnalyticsPerformance(filters = {}) {
  const { data } = await axiosInstance.get(`${BASE}/performance`, {
    params: buildParams(filters),
  });
  return data;
}

/**
 * Fetches department headcount analytics.
 * ADMIN / HR / MANAGER only.
 *
 * @returns {Promise<AnalyticsDepartments>}
 */
export async function getAnalyticsDepartments() {
  const { data } = await axiosInstance.get(`${BASE}/departments`);
  return data;
}
