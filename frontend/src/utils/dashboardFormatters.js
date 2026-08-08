/**
 * @fileoverview Dashboard data formatting helpers.
 *
 * Pure functions — no side effects, no React imports. Each function
 * takes raw API data and returns a display-ready string or object.
 */

/**
 * Formats a raw number as a compact string (e.g., 1500 → "1.5K").
 *
 * @param {number} value - The raw number.
 * @returns {string} The formatted string.
 */
export function formatCompactNumber(value) {
  if (value == null) return '—';
  if (value >= 1_000_000) return `${(value / 1_000_000).toFixed(1)}M`;
  if (value >= 1_000)     return `${(value / 1_000).toFixed(1)}K`;
  return String(value);
}

/**
 * Formats a decimal value as a percentage string.
 *
 * @param {number} value     - Value between 0 and 1 (or 0–100 if {@code isRatio} is false).
 * @param {boolean} [isRatio=true] - Whether {@code value} is already a 0–1 ratio.
 * @param {number}  [decimals=1]   - Decimal places to include.
 * @returns {string} e.g., "87.5%"
 */
export function formatPercent(value, isRatio = true, decimals = 1) {
  if (value == null) return '—';
  const pct = isRatio ? value * 100 : value;
  return `${pct.toFixed(decimals)}%`;
}

/**
 * Calculates an attendance rate as a percentage string.
 *
 * @param {number} present - Number of employees present.
 * @param {number} total   - Total employees.
 * @returns {string} e.g., "92.0%"
 */
export function calcAttendanceRate(present, total) {
  if (!total) return '0.0%';
  return formatPercent(present / total);
}

/**
 * Derives a MUI colour token from a numeric trend change value.
 * Positive → success, zero → text.secondary, negative → error.
 *
 * @param {number} change - The trend change value.
 * @returns {'success.main' | 'text.secondary' | 'error.main'} MUI colour path.
 */
export function trendColor(change) {
  if (change > 0)  return 'success.main';
  if (change < 0)  return 'error.main';
  return 'text.secondary';
}

/**
 * Formats a trend change number with a leading + or − sign.
 *
 * @param {number} change - The raw change value.
 * @returns {string} e.g., "+12", "−3", "0"
 */
export function formatTrend(change) {
  if (change == null) return '';
  if (change > 0) return `+${change}`;
  if (change < 0) return `−${Math.abs(change)}`;
  return '0';
}

/**
 * Formats a duration in minutes into a human-readable "Xh Ym" string.
 *
 * @param {number} minutes - Total minutes.
 * @returns {string} e.g., "8h 30m"
 */
export function formatDuration(minutes) {
  if (!minutes) return '—';
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  if (h === 0) return `${m}m`;
  if (m === 0) return `${h}h`;
  return `${h}h ${m}m`;
}

/**
 * Builds Recharts-compatible data from a department distribution record.
 *
 * @param {Array<{name: string, count: number}>} departments
 * @returns {Array<{name: string, value: number}>}
 */
export function toPieChartData(departments) {
  if (!Array.isArray(departments)) return [];
  return departments.map((d) => ({ name: d.name, value: d.count }));
}

/**
 * Converts an attendance trend array into a Recharts line chart dataset.
 *
 * @param {Array<{date: string, present: number, absent: number}>} trend
 * @returns {Array<{date: string, Present: number, Absent: number}>}
 */
export function toAttendanceLineData(trend) {
  if (!Array.isArray(trend)) return [];
  return trend.map((t) => ({
    date:    t.date,
    Present: t.present,
    Absent:  t.absent,
  }));
}
