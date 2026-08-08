/**
 * @fileoverview Leave module formatting utilities.
 *
 * Pure functions — no side effects, no React imports.
 */

import dayjs from 'dayjs';
import { formatDate } from '@/utils/dateUtils';
import {
  LEAVE_TYPE_MAP,
  LEAVE_STATUS_MAP,
} from '@/constants/leaveConstants';
import { countWorkingDays } from '@/utils/leaveCalculations';

// ── Type + status labels ──────────────────────────────────────────────────────

/**
 * Returns the human-readable label for a leave type.
 *
 * @param {string | null | undefined} type
 * @returns {string}
 */
export function formatLeaveType(type) {
  return LEAVE_TYPE_MAP[type]?.label ?? type ?? '—';
}

/**
 * Returns the human-readable label for a leave status.
 *
 * @param {string | null | undefined} status
 * @returns {string}
 */
export function formatLeaveStatus(status) {
  return LEAVE_STATUS_MAP[status]?.label ?? status ?? '—';
}

// ── Date formatting ───────────────────────────────────────────────────────────

/**
 * Formats a leave date range as "Jan 15 – Jan 20, 2024".
 *
 * @param {string | null | undefined} startDate
 * @param {string | null | undefined} endDate
 * @returns {string}
 */
export function formatLeaveDateRange(startDate, endDate) {
  if (!startDate && !endDate) return '—';
  const s = startDate ? dayjs(startDate).format('MMM D') : '?';
  const e = endDate   ? dayjs(endDate).format('MMM D, YYYY') : '?';
  return `${s} – ${e}`;
}

/**
 * Returns a short relative label like "In 3 days" or "Yesterday".
 *
 * @param {string | null | undefined} startDate
 * @returns {string}
 */
export function formatLeaveStartRelative(startDate) {
  if (!startDate) return '—';
  const diff = dayjs(startDate).diff(dayjs(), 'day');
  if (diff === 0)  return 'Today';
  if (diff === 1)  return 'Tomorrow';
  if (diff === -1) return 'Yesterday';
  if (diff > 0)    return `In ${diff} days`;
  return `${Math.abs(diff)} days ago`;
}

// ── Day count formatting ──────────────────────────────────────────────────────

/**
 * Returns a formatted working-day count string.
 *
 * @param {number | null | undefined} days
 * @returns {string} e.g., "3 days", "1 day", "—"
 */
export function formatLeaveDays(days) {
  if (days == null) return '—';
  return `${days} ${days === 1 ? 'day' : 'days'}`;
}

/**
 * Computes and formats working days from a leave request's date range.
 *
 * @param {string} startDate
 * @param {string} endDate
 * @returns {string}
 */
export function formatLeaveWorkingDays(startDate, endDate) {
  const days = countWorkingDays(startDate, endDate);
  return formatLeaveDays(days);
}

// ── CSV builder ───────────────────────────────────────────────────────────────

/**
 * Converts leave requests to a CSV string.
 *
 * @param {import('@/services/leaveApi').LeaveRequestResponse[]} leaves
 * @param {string[]} headers
 * @param {string[]} fields
 * @returns {string}
 */
export function buildLeaveCsvString(leaves, headers, fields) {
  const escape = (val) => {
    const str = val == null ? '' : String(val);
    return `"${str.replace(/"/g, '""')}"`;
  };
  const headerRow = headers.map(escape).join(',');
  const dataRows  = leaves.map((l) =>
    fields.map((k) => escape(l[k])).join(','),
  );
  return [headerRow, ...dataRows].join('\r\n');
}

/**
 * Triggers a browser CSV download.
 *
 * @param {string} csvContent
 * @param {string} [filename='leaves.csv']
 * @returns {void}
 */
export function downloadLeaveCsv(csvContent, filename = 'leaves.csv') {
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
  const url  = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href     = url;
  link.download = filename;
  link.style.display = 'none';
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}

// ── Calendar event builder ────────────────────────────────────────────────────

/**
 * @typedef {Object} CalendarEvent
 * @property {string} id
 * @property {string} title
 * @property {string} start       - 'YYYY-MM-DD'
 * @property {string} end         - 'YYYY-MM-DD'
 * @property {string} color       - Background hex colour.
 * @property {string} status
 * @property {string} leaveType
 */

/**
 * Converts a leave request into a calendar event object.
 *
 * @param {import('@/services/leaveApi').LeaveRequestResponse} leave
 * @param {string} color - Hex colour from LEAVE_CALENDAR_COLORS.
 * @returns {CalendarEvent}
 */
export function toCalendarEvent(leave, color) {
  const typeMeta = LEAVE_TYPE_MAP[leave.leaveType];
  return {
    id:        leave.id,
    title:     `${typeMeta?.icon ?? ''} ${leave.employeeName ?? formatLeaveType(leave.leaveType)}`,
    start:     leave.startDate,
    end:       leave.endDate,
    color,
    status:    leave.status,
    leaveType: leave.leaveType,
  };
}
