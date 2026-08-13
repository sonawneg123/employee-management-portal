/**
 * @fileoverview Department module formatting utilities.
 *
 * Pure functions — no side-effects, no React imports.
 * All display-formatting logic for the department module lives here.
 */

import { formatDate } from '@/utils/dateUtils';
import { DEPT_AVATAR_COLORS } from '@/constants/departmentConstants';

// ── Avatar helpers ────────────────────────────────────────────────────────────

/**
 * Returns up to two uppercase initials from a department name.
 *
 * @param {string | null | undefined} name - Department name.
 * @returns {string} e.g., "EN" for "Engineering", "?" for missing.
 */
export function deptInitials(name) {
  if (!name) return '?';
  const words = name.trim().split(/\s+/);
  if (words.length === 1) return words[0].slice(0, 2).toUpperCase();
  return (words[0][0] + words[1][0]).toUpperCase();
}

/**
 * Deterministically picks a background colour for a department avatar.
 *
 * @param {string | null | undefined} name - Department name.
 * @returns {string} A hex colour string.
 */
export function deptAvatarColor(name) {
  if (!name) return DEPT_AVATAR_COLORS[0];
  return DEPT_AVATAR_COLORS[name.charCodeAt(0) % DEPT_AVATAR_COLORS.length];
}

// ── Count formatting ──────────────────────────────────────────────────────────

/**
 * Formats an employee count as a human-readable string.
 *
 * @param {number | null | undefined} count
 * @returns {string} e.g., "42 employees", "1 employee", "—".
 */
export function formatEmployeeCount(count) {
  if (count == null) return '—';
  return `${count} ${count === 1 ? 'employee' : 'employees'}`;
}

// ── Date formatting ───────────────────────────────────────────────────────────

/**
 * Formats the department's createdAt timestamp for display.
 *
 * @param {string | null | undefined} createdAt - ISO datetime string.
 * @returns {string} e.g., "Jan 15, 2022" or "—".
 */
export function formatDeptCreatedAt(createdAt) {
  return formatDate(createdAt);
}

// ── Code badge ────────────────────────────────────────────────────────────────

/**
 * Returns the department code in uppercase, or "—" if absent.
 *
 * @param {string | null | undefined} code
 * @returns {string}
 */
export function formatDeptCode(code) {
  return code ? code.toUpperCase() : '—';
}

// ── Head name ─────────────────────────────────────────────────────────────────

/**
 * Returns the department head's name or "—" if unassigned.
 *
 * @param {string | null | undefined} headName
 * @returns {string}
 */
export function formatHeadName(headName) {
  return headName?.trim() || '—';
}

// ── CSV builder ───────────────────────────────────────────────────────────────

/**
 * Converts an array of DepartmentResponse objects into a CSV string.
 *
 * @param {import('@/services/departmentApi').DepartmentResponse[]} departments
 * @param {string[]} headers - Column header labels.
 * @param {string[]} fields  - DepartmentResponse keys matching headers.
 * @returns {string} Full CSV text ready for download.
 */
export function buildDeptCsvString(departments, headers, fields) {
  const escape = (val) => {
    const str = val == null ? '' : String(val);
    return `"${str.replace(/"/g, '""')}"`;
  };
  const headerRow = headers.map(escape).join(',');
  const dataRows = departments.map((d) => fields.map((k) => escape(d[k])).join(','));
  return [headerRow, ...dataRows].join('\r\n');
}

/**
 * Triggers a browser download of the given CSV text.
 *
 * @param {string} csvContent
 * @param {string} [filename='departments.csv']
 * @returns {void}
 */
export function downloadDeptCsv(csvContent, filename = 'departments.csv') {
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  link.style.display = 'none';
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}
