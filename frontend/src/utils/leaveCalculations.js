/**
 * @fileoverview Leave day calculation utilities.
 *
 * Pure functions — no side effects, no React.
 * Used to compute working-day counts, validate date ranges, and build
 * summary statistics for the leave management module.
 */

import dayjs from 'dayjs';
import isSameOrBefore from 'dayjs/plugin/isSameOrBefore';
import isBetween from 'dayjs/plugin/isBetween';

dayjs.extend(isSameOrBefore);
dayjs.extend(isBetween);

// ── Working day calculation ───────────────────────────────────────────────────

/**
 * Counts the number of working days (Mon–Fri) between two dates, inclusive.
 *
 * @param {string | Date | import('dayjs').Dayjs} startDate
 * @param {string | Date | import('dayjs').Dayjs} endDate
 * @param {string[]} [publicHolidays=[]] - Array of 'YYYY-MM-DD' holiday strings to exclude.
 * @returns {number} Working day count, or 0 if dates are invalid or reversed.
 */
export function countWorkingDays(startDate, endDate, publicHolidays = []) {
  const start = dayjs(startDate).startOf('day');
  const end = dayjs(endDate).startOf('day');

  if (!start.isValid() || !end.isValid()) return 0;
  if (end.isBefore(start)) return 0;

  const holidaySet = new Set(publicHolidays);
  let count = 0;
  let current = start;

  while (current.isSameOrBefore(end, 'day')) {
    const dayOfWeek = current.day(); // 0=Sun, 6=Sat
    const dateStr = current.format('YYYY-MM-DD');
    if (dayOfWeek !== 0 && dayOfWeek !== 6 && !holidaySet.has(dateStr)) {
      count++;
    }
    current = current.add(1, 'day');
  }

  return count;
}

/**
 * Counts calendar days (all days, including weekends) between two dates, inclusive.
 *
 * @param {string | Date} startDate
 * @param {string | Date} endDate
 * @returns {number}
 */
export function countCalendarDays(startDate, endDate) {
  const start = dayjs(startDate);
  const end = dayjs(endDate);
  if (!start.isValid() || !end.isValid() || end.isBefore(start)) return 0;
  return end.diff(start, 'day') + 1;
}

// ── Date range validation ─────────────────────────────────────────────────────

/**
 * Validates a leave date range.
 *
 * @param {string} startDate - ISO date string.
 * @param {string} endDate   - ISO date string.
 * @returns {{ valid: boolean, message: string }} Validation result.
 */
export function validateDateRange(startDate, endDate) {
  const start = dayjs(startDate);
  const end = dayjs(endDate);

  if (!start.isValid()) return { valid: false, message: 'Start date is invalid.' };
  if (!end.isValid()) return { valid: false, message: 'End date is invalid.' };
  if (end.isBefore(start)) {
    return { valid: false, message: 'End date must be on or after start date.' };
  }
  if (start.isBefore(dayjs().startOf('day'))) {
    return { valid: false, message: 'Start date cannot be in the past.' };
  }

  return { valid: true, message: '' };
}

/**
 * Returns whether a given date falls within a leave request's range.
 *
 * @param {string | Date} date
 * @param {string | Date} startDate
 * @param {string | Date} endDate
 * @returns {boolean}
 */
export function isDateInLeaveRange(date, startDate, endDate) {
  const d = dayjs(date);
  const start = dayjs(startDate);
  const end = dayjs(endDate);
  if (!d.isValid() || !start.isValid() || !end.isValid()) return false;
  return d.isBetween(start, end, 'day', '[]');
}

// ── Leave overlap detection ───────────────────────────────────────────────────

/**
 * Checks whether two leave date ranges overlap.
 *
 * @param {{ startDate: string, endDate: string }} rangeA
 * @param {{ startDate: string, endDate: string }} rangeB
 * @returns {boolean}
 */
export function doLeavesOverlap(rangeA, rangeB) {
  const aStart = dayjs(rangeA.startDate);
  const aEnd = dayjs(rangeA.endDate);
  const bStart = dayjs(rangeB.startDate);
  const bEnd = dayjs(rangeB.endDate);

  // Overlap if A starts before B ends AND B starts before A ends
  return aStart.isSameOrBefore(bEnd, 'day') && bStart.isSameOrBefore(aEnd, 'day');
}

// ── Leave statistics ──────────────────────────────────────────────────────────

/**
 * @typedef {Object} LeaveStatsByType
 * @property {string} type  - LeaveType enum value.
 * @property {number} count - Number of requests.
 * @property {number} days  - Total working days requested.
 */

/**
 * Aggregates leave requests by type.
 *
 * @param {import('@/services/leaveApi').LeaveRequestResponse[]} leaves
 * @returns {LeaveStatsByType[]}
 */
export function aggregateByType(leaves) {
  /** @type {Record<string, LeaveStatsByType>} */
  const acc = {};

  leaves.forEach((leave) => {
    const type = leave.leaveType;
    if (!acc[type]) acc[type] = { type, count: 0, days: 0 };
    acc[type].count++;
    acc[type].days += countWorkingDays(leave.startDate, leave.endDate);
  });

  return Object.values(acc).sort((a, b) => b.count - a.count);
}

/**
 * @typedef {Object} LeaveStatsByStatus
 * @property {string} status
 * @property {number} count
 */

/**
 * Aggregates leave requests by status.
 *
 * @param {import('@/services/leaveApi').LeaveRequestResponse[]} leaves
 * @returns {LeaveStatsByStatus[]}
 */
export function aggregateByStatus(leaves) {
  /** @type {Record<string, number>} */
  const acc = {};

  leaves.forEach((leave) => {
    acc[leave.status] = (acc[leave.status] ?? 0) + 1;
  });

  return Object.entries(acc)
    .map(([status, count]) => ({ status, count }))
    .sort((a, b) => b.count - a.count);
}

// ── Leave balance ─────────────────────────────────────────────────────────────

/**
 * Calculates used days for a specific leave type from a list of APPROVED leaves.
 *
 * @param {import('@/services/leaveApi').LeaveRequestResponse[]} approvedLeaves
 * @param {string} leaveType - LeaveType enum value to filter by.
 * @returns {number} Total working days used.
 */
export function usedDaysByType(approvedLeaves, leaveType) {
  return approvedLeaves
    .filter((l) => l.leaveType === leaveType && l.status === 'APPROVED')
    .reduce((sum, l) => sum + countWorkingDays(l.startDate, l.endDate), 0);
}
