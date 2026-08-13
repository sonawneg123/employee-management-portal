/**
 * @fileoverview Tests for dashboard utility functions in dashboardFormatters.js.
 *
 * Covers all exported functions:
 *   - formatCompactNumber
 *   - formatPercent
 *   - calcAttendanceRate
 *   - trendColor
 *   - formatTrend
 *   - formatDuration
 *   - toPieChartData
 *   - toAttendanceLineData
 */

import { describe, it, expect } from 'vitest';
import {
  formatCompactNumber,
  formatPercent,
  calcAttendanceRate,
  trendColor,
  formatTrend,
  formatDuration,
  toPieChartData,
  toAttendanceLineData,
} from '@/utils/dashboardFormatters';

// ── formatCompactNumber ───────────────────────────────────────────────────────

describe('formatCompactNumber', () => {
  it('formats numbers below 1K as plain strings', () => {
    expect(formatCompactNumber(0)).toBe('0');
    expect(formatCompactNumber(42)).toBe('42');
    expect(formatCompactNumber(999)).toBe('999');
  });

  it('formats thousands with K suffix', () => {
    expect(formatCompactNumber(1000)).toBe('1.0K');
    expect(formatCompactNumber(1500)).toBe('1.5K');
    expect(formatCompactNumber(999999)).toBe('1000.0K');
  });

  it('formats millions with M suffix', () => {
    expect(formatCompactNumber(1_000_000)).toBe('1.0M');
    expect(formatCompactNumber(2_500_000)).toBe('2.5M');
  });

  it('returns em dash for null/undefined', () => {
    expect(formatCompactNumber(null)).toBe('—');
    expect(formatCompactNumber(undefined)).toBe('—');
  });
});

// ── formatPercent ─────────────────────────────────────────────────────────────

describe('formatPercent', () => {
  it('formats a ratio (0–1) as a percentage', () => {
    expect(formatPercent(0.875)).toBe('87.5%');
    expect(formatPercent(1)).toBe('100.0%');
    expect(formatPercent(0)).toBe('0.0%');
  });

  it('formats a raw 0–100 value when isRatio=false', () => {
    expect(formatPercent(87.5, false)).toBe('87.5%');
  });

  it('respects the decimals parameter', () => {
    expect(formatPercent(0.8765, true, 2)).toBe('87.65%');
    expect(formatPercent(0.8765, true, 0)).toBe('88%');
  });

  it('returns em dash for null/undefined', () => {
    expect(formatPercent(null)).toBe('—');
  });
});

// ── calcAttendanceRate ────────────────────────────────────────────────────────

describe('calcAttendanceRate', () => {
  it('calculates correct percentage', () => {
    expect(calcAttendanceRate(9, 10)).toBe('90.0%');
    expect(calcAttendanceRate(10, 10)).toBe('100.0%');
  });

  it('returns 0.0% when total is 0', () => {
    expect(calcAttendanceRate(0, 0)).toBe('0.0%');
  });

  it('returns 0.0% when present is 0', () => {
    expect(calcAttendanceRate(0, 50)).toBe('0.0%');
  });
});

// ── trendColor ────────────────────────────────────────────────────────────────

describe('trendColor', () => {
  it('returns success.main for positive values', () => {
    expect(trendColor(5)).toBe('success.main');
    expect(trendColor(0.1)).toBe('success.main');
  });

  it('returns error.main for negative values', () => {
    expect(trendColor(-3)).toBe('error.main');
  });

  it('returns text.secondary for zero', () => {
    expect(trendColor(0)).toBe('text.secondary');
  });
});

// ── formatTrend ───────────────────────────────────────────────────────────────

describe('formatTrend', () => {
  it('prepends + for positive changes', () => {
    expect(formatTrend(12)).toBe('+12');
  });

  it('uses minus sign for negative changes', () => {
    expect(formatTrend(-3)).toBe('−3');
  });

  it('returns "0" for zero', () => {
    expect(formatTrend(0)).toBe('0');
  });

  it('returns empty string for null/undefined', () => {
    expect(formatTrend(null)).toBe('');
    expect(formatTrend(undefined)).toBe('');
  });
});

// ── formatDuration ────────────────────────────────────────────────────────────

describe('formatDuration', () => {
  it('formats hours and minutes', () => {
    expect(formatDuration(510)).toBe('8h 30m');
    expect(formatDuration(90)).toBe('1h 30m');
  });

  it('formats whole hours only', () => {
    expect(formatDuration(60)).toBe('1h');
    expect(formatDuration(120)).toBe('2h');
  });

  it('formats minutes only when under 60', () => {
    expect(formatDuration(45)).toBe('45m');
  });

  it('returns em dash for 0 or falsy', () => {
    expect(formatDuration(0)).toBe('—');
    expect(formatDuration(null)).toBe('—');
  });
});

// ── toPieChartData ────────────────────────────────────────────────────────────

describe('toPieChartData', () => {
  it('maps department array to Recharts format', () => {
    const input = [
      { name: 'Engineering', count: 20, code: 'ENG' },
      { name: 'HR', count: 5, code: 'HR' },
    ];
    expect(toPieChartData(input)).toEqual([
      { name: 'Engineering', value: 20 },
      { name: 'HR', value: 5 },
    ]);
  });

  it('returns empty array for non-array input', () => {
    expect(toPieChartData(null)).toEqual([]);
    expect(toPieChartData(undefined)).toEqual([]);
    expect(toPieChartData('bad')).toEqual([]);
  });

  it('returns empty array for empty input', () => {
    expect(toPieChartData([])).toEqual([]);
  });
});

// ── toAttendanceLineData ──────────────────────────────────────────────────────

describe('toAttendanceLineData', () => {
  it('converts raw trend data to Recharts format', () => {
    const input = [
      { date: '2024-01-14', present: 37, absent: 5 },
      { date: '2024-01-15', present: 38, absent: 4 },
    ];
    expect(toAttendanceLineData(input)).toEqual([
      { date: '2024-01-14', Present: 37, Absent: 5 },
      { date: '2024-01-15', Present: 38, Absent: 4 },
    ]);
  });

  it('returns empty array for non-array input', () => {
    expect(toAttendanceLineData(null)).toEqual([]);
    expect(toAttendanceLineData(undefined)).toEqual([]);
  });
});
