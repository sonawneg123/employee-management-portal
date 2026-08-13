/**
 * @fileoverview Employee module formatting utilities.
 *
 * Pure functions with no side-effects. All formatting logic for the
 * employee module lives here to avoid duplication across components.
 */

import { formatDate } from '@/utils/dateUtils';

// ── Name formatting ───────────────────────────────────────────────────────────

/**
 * Combines first and last name into a single display string.
 *
 * @param {string | null | undefined} firstName
 * @param {string | null | undefined} lastName
 * @returns {string} e.g., "Jane Smith" or "—" if both are absent.
 */
export function formatFullName(firstName, lastName) {
  const parts = [firstName, lastName].filter(Boolean);
  return parts.length > 0 ? parts.join(' ') : '—';
}

/**
 * Returns the initials (up to 2 characters) from a full name.
 * Used for MUI Avatar fallback text.
 *
 * @param {string | null | undefined} firstName
 * @param {string | null | undefined} lastName
 * @returns {string} e.g., "JS" or "?" if no name is provided.
 */
export function formatInitials(firstName, lastName) {
  const f = firstName?.charAt(0)?.toUpperCase() ?? '';
  const l = lastName?.charAt(0)?.toUpperCase() ?? '';
  return `${f}${l}` || '?';
}

// ── Salary formatting ─────────────────────────────────────────────────────────

/**
 * Formats a numeric salary as a USD currency string.
 *
 * @param {number | null | undefined} salary - Raw salary value.
 * @param {string} [currency='USD']          - ISO currency code.
 * @returns {string} e.g., "$75,000.00" or "—" if null.
 */
export function formatSalary(salary, currency = 'USD') {
  if (salary == null) return '—';
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency,
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(salary);
}

// ── Date of joining ───────────────────────────────────────────────────────────

/**
 * Formats a date-of-joining string for display.
 *
 * @param {string | null | undefined} dateOfJoining - ISO date string.
 * @returns {string} e.g., "Jan 15, 2022" or "—".
 */
export function formatJoinDate(dateOfJoining) {
  return formatDate(dateOfJoining);
}

// ── Phone formatting ──────────────────────────────────────────────────────────

/**
 * Returns the phone number or an em dash if absent.
 *
 * @param {string | null | undefined} phone
 * @returns {string}
 */
export function formatPhone(phone) {
  return phone?.trim() || '—';
}

// ── CSV export ────────────────────────────────────────────────────────────────

/**
 * Converts an array of EmployeeResponse objects into a CSV string.
 * All fields are quoted to handle commas in values.
 *
 * @param {import('@/services/employeeApi').EmployeeResponse[]} employees
 * @param {string[]} headers  - Column headers row.
 * @param {string[]} fields   - EmployeeResponse field keys matching headers.
 * @returns {string} Full CSV text ready for download.
 */
export function buildCsvString(employees, headers, fields) {
  const escapeCell = (val) => {
    const str = val == null ? '' : String(val);
    // Wrap in quotes; double any existing quotes
    return `"${str.replace(/"/g, '""')}"`;
  };

  const headerRow = headers.map(escapeCell).join(',');
  const dataRows = employees.map((emp) => fields.map((key) => escapeCell(emp[key])).join(','));

  return [headerRow, ...dataRows].join('\r\n');
}

/**
 * Triggers a browser download of the given CSV content.
 *
 * @param {string} csvContent  - The full CSV string.
 * @param {string} [filename='employees.csv'] - Download filename.
 * @returns {void}
 */
export function downloadCsv(csvContent, filename = 'employees.csv') {
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

// ── Avatar colour ─────────────────────────────────────────────────────────────

/**
 * Deterministically picks one of eight avatar background colours based on
 * the first character of a name, so the same person always gets the same colour.
 *
 * @param {string | null | undefined} name - Any name string.
 * @returns {string} A hex colour string.
 */
export function avatarColorFromName(name) {
  const COLORS = [
    '#1976d2',
    '#7c3aed',
    '#2e7d32',
    '#ed6c02',
    '#0288d1',
    '#c62828',
    '#00796b',
    '#f57f17',
  ];
  if (!name) return COLORS[0];
  const code = name.charCodeAt(0) % COLORS.length;
  return COLORS[code];
}

// ── Status label ──────────────────────────────────────────────────────────────

/**
 * Converts a raw status enum value to a title-cased display label.
 *
 * @param {string | null | undefined} status - e.g., "ON_LEAVE"
 * @returns {string} e.g., "On Leave"
 */
export function formatStatusLabel(status) {
  if (!status) return '—';
  return status
    .split('_')
    .map((w) => w.charAt(0) + w.slice(1).toLowerCase())
    .join(' ');
}

// ── Years of service ──────────────────────────────────────────────────────────

/**
 * Calculates years of service from a dateOfJoining string.
 *
 * @param {string | null | undefined} dateOfJoining - ISO date string.
 * @returns {string} e.g., "3 yrs" or "< 1 yr".
 */
export function formatYearsOfService(dateOfJoining) {
  if (!dateOfJoining) return '—';
  const join = new Date(dateOfJoining);
  const now = new Date();
  const years = Math.floor((now - join) / (365.25 * 24 * 60 * 60 * 1000));
  if (years < 1) return '< 1 yr';
  return `${years} ${years === 1 ? 'yr' : 'yrs'}`;
}
