/**
 * @fileoverview Unit tests for leaveCalculations.js
 *
 * Covers:
 *   - countWorkingDays — basic Mon–Fri counting, weekends, public holidays, edge cases
 *   - countCalendarDays — inclusive day count
 *   - validateDateRange — valid, end-before-start, past-start, invalid input
 *   - isDateInLeaveRange — inclusive boundary checks
 *   - doLeavesOverlap — overlapping and non-overlapping ranges
 *   - aggregateByType — groups and sums working days
 *   - aggregateByStatus — groups by status
 *   - usedDaysByType — filters by type + APPROVED status
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import dayjs from 'dayjs';
import {
  countWorkingDays,
  countCalendarDays,
  validateDateRange,
  isDateInLeaveRange,
  doLeavesOverlap,
  aggregateByType,
  aggregateByStatus,
  usedDaysByType,
} from '@/utils/leaveCalculations';

// ── countWorkingDays ─────────────────────────────────────────────────────────

describe('countWorkingDays', () => {
  it('counts Mon–Fri in a simple week (5 days)', () => {
    // 2024-01-08 (Mon) → 2024-01-12 (Fri)
    expect(countWorkingDays('2024-01-08', '2024-01-12')).toBe(5);
  });

  it('excludes Saturday and Sunday', () => {
    // 2024-01-08 (Mon) → 2024-01-14 (Sun) = 5 working days
    expect(countWorkingDays('2024-01-08', '2024-01-14')).toBe(5);
  });

  it('counts a single Monday as 1', () => {
    expect(countWorkingDays('2024-01-08', '2024-01-08')).toBe(1);
  });

  it('returns 0 for a single Saturday', () => {
    expect(countWorkingDays('2024-01-13', '2024-01-13')).toBe(0);
  });

  it('returns 0 for a single Sunday', () => {
    expect(countWorkingDays('2024-01-14', '2024-01-14')).toBe(0);
  });

  it('returns 0 when end is before start', () => {
    expect(countWorkingDays('2024-01-12', '2024-01-08')).toBe(0);
  });

  it('returns 0 for invalid dates', () => {
    expect(countWorkingDays('invalid', '2024-01-12')).toBe(0);
    expect(countWorkingDays('2024-01-08', 'invalid')).toBe(0);
  });

  it('excludes public holidays from working days', () => {
    // 2024-01-08 Mon → 2024-01-12 Fri = 5 days, minus 1 holiday on Wed
    expect(countWorkingDays('2024-01-08', '2024-01-12', ['2024-01-10'])).toBe(4);
  });

  it('handles two-week span correctly', () => {
    // 2024-01-08 (Mon) → 2024-01-19 (Fri) = 10 working days
    expect(countWorkingDays('2024-01-08', '2024-01-19')).toBe(10);
  });
});

// ── countCalendarDays ────────────────────────────────────────────────────────

describe('countCalendarDays', () => {
  it('counts 7 days in one full week', () => {
    expect(countCalendarDays('2024-01-08', '2024-01-14')).toBe(7);
  });

  it('returns 1 for same-day range', () => {
    expect(countCalendarDays('2024-01-10', '2024-01-10')).toBe(1);
  });

  it('returns 0 when end is before start', () => {
    expect(countCalendarDays('2024-01-12', '2024-01-08')).toBe(0);
  });

  it('returns 0 for invalid dates', () => {
    expect(countCalendarDays('bad', '2024-01-10')).toBe(0);
  });
});

// ── validateDateRange ────────────────────────────────────────────────────────

describe('validateDateRange', () => {
  it('returns valid for today → next week', () => {
    const start = dayjs().format('YYYY-MM-DD');
    const end = dayjs().add(7, 'day').format('YYYY-MM-DD');
    const result = validateDateRange(start, end);
    expect(result.valid).toBe(true);
    expect(result.message).toBe('');
  });

  it('returns invalid when start date is in the past', () => {
    const past = dayjs().subtract(1, 'day').format('YYYY-MM-DD');
    const end = dayjs().add(3, 'day').format('YYYY-MM-DD');
    const result = validateDateRange(past, end);
    expect(result.valid).toBe(false);
    expect(result.message).toMatch(/past/i);
  });

  it('returns invalid when end is before start', () => {
    const start = dayjs().add(5, 'day').format('YYYY-MM-DD');
    const end = dayjs().add(2, 'day').format('YYYY-MM-DD');
    const result = validateDateRange(start, end);
    expect(result.valid).toBe(false);
    expect(result.message).toMatch(/on or after/i);
  });

  it('returns invalid for invalid start date', () => {
    const result = validateDateRange('not-a-date', '2025-01-10');
    expect(result.valid).toBe(false);
    expect(result.message).toMatch(/start date is invalid/i);
  });

  it('returns invalid for invalid end date', () => {
    const start = dayjs().add(1, 'day').format('YYYY-MM-DD');
    const result = validateDateRange(start, 'not-a-date');
    expect(result.valid).toBe(false);
    expect(result.message).toMatch(/end date is invalid/i);
  });
});

// ── isDateInLeaveRange ───────────────────────────────────────────────────────

describe('isDateInLeaveRange', () => {
  const start = '2024-01-10';
  const end = '2024-01-15';

  it('returns true for a date inside the range', () => {
    expect(isDateInLeaveRange('2024-01-12', start, end)).toBe(true);
  });

  it('returns true on the start boundary', () => {
    expect(isDateInLeaveRange('2024-01-10', start, end)).toBe(true);
  });

  it('returns true on the end boundary', () => {
    expect(isDateInLeaveRange('2024-01-15', start, end)).toBe(true);
  });

  it('returns false before the range', () => {
    expect(isDateInLeaveRange('2024-01-09', start, end)).toBe(false);
  });

  it('returns false after the range', () => {
    expect(isDateInLeaveRange('2024-01-16', start, end)).toBe(false);
  });

  it('returns false for invalid date', () => {
    expect(isDateInLeaveRange('bad', start, end)).toBe(false);
  });
});

// ── doLeavesOverlap ──────────────────────────────────────────────────────────

describe('doLeavesOverlap', () => {
  const A = { startDate: '2024-01-10', endDate: '2024-01-15' };

  it('detects overlap when B is fully inside A', () => {
    expect(doLeavesOverlap(A, { startDate: '2024-01-11', endDate: '2024-01-13' })).toBe(true);
  });

  it('detects overlap when B starts inside A and ends after', () => {
    expect(doLeavesOverlap(A, { startDate: '2024-01-13', endDate: '2024-01-20' })).toBe(true);
  });

  it('detects overlap when B starts before A and ends inside', () => {
    expect(doLeavesOverlap(A, { startDate: '2024-01-05', endDate: '2024-01-11' })).toBe(true);
  });

  it('detects overlap when ranges share only the boundary day', () => {
    expect(doLeavesOverlap(A, { startDate: '2024-01-15', endDate: '2024-01-20' })).toBe(true);
    expect(doLeavesOverlap(A, { startDate: '2024-01-05', endDate: '2024-01-10' })).toBe(true);
  });

  it('returns false for completely non-overlapping ranges', () => {
    expect(doLeavesOverlap(A, { startDate: '2024-01-16', endDate: '2024-01-20' })).toBe(false);
    expect(doLeavesOverlap(A, { startDate: '2024-01-01', endDate: '2024-01-09' })).toBe(false);
  });
});

// ── aggregateByType ──────────────────────────────────────────────────────────

describe('aggregateByType', () => {
  const leaves = [
    { leaveType: 'ANNUAL', status: 'APPROVED', startDate: '2024-01-08', endDate: '2024-01-08' },
    { leaveType: 'ANNUAL', status: 'PENDING', startDate: '2024-01-09', endDate: '2024-01-09' },
    { leaveType: 'SICK', status: 'APPROVED', startDate: '2024-01-10', endDate: '2024-01-10' },
  ];

  it('returns one entry per leave type', () => {
    const result = aggregateByType(leaves);
    const types = result.map((r) => r.type);
    expect(types).toContain('ANNUAL');
    expect(types).toContain('SICK');
    expect(types.length).toBe(2);
  });

  it('counts requests per type correctly', () => {
    const result = aggregateByType(leaves);
    const annual = result.find((r) => r.type === 'ANNUAL');
    expect(annual.count).toBe(2);
  });

  it('accumulates working days per type', () => {
    const result = aggregateByType(leaves);
    const annual = result.find((r) => r.type === 'ANNUAL');
    expect(annual.days).toBe(2); // 1 day + 1 day
  });

  it('returns empty array for no leaves', () => {
    expect(aggregateByType([])).toEqual([]);
  });
});

// ── aggregateByStatus ────────────────────────────────────────────────────────

describe('aggregateByStatus', () => {
  const leaves = [
    { status: 'PENDING' },
    { status: 'PENDING' },
    { status: 'APPROVED' },
    { status: 'REJECTED' },
  ];

  it('groups correctly', () => {
    const result = aggregateByStatus(leaves);
    const pending = result.find((r) => r.status === 'PENDING');
    expect(pending.count).toBe(2);
  });

  it('returns empty array for no leaves', () => {
    expect(aggregateByStatus([])).toEqual([]);
  });
});

// ── usedDaysByType ───────────────────────────────────────────────────────────

describe('usedDaysByType', () => {
  const leaves = [
    { leaveType: 'ANNUAL', status: 'APPROVED', startDate: '2024-01-08', endDate: '2024-01-12' }, // 5 days
    { leaveType: 'ANNUAL', status: 'PENDING', startDate: '2024-01-15', endDate: '2024-01-15' }, // not APPROVED
    { leaveType: 'SICK', status: 'APPROVED', startDate: '2024-01-08', endDate: '2024-01-08' }, // 1 day
  ];

  it('sums working days for APPROVED leaves of the given type', () => {
    expect(usedDaysByType(leaves, 'ANNUAL')).toBe(5);
  });

  it('excludes PENDING leaves', () => {
    // ANNUAL PENDING should not be counted
    expect(usedDaysByType(leaves, 'ANNUAL')).toBe(5);
  });

  it('counts SICK days correctly', () => {
    expect(usedDaysByType(leaves, 'SICK')).toBe(1);
  });

  it('returns 0 when no leaves match the type', () => {
    expect(usedDaysByType(leaves, 'MATERNITY')).toBe(0);
  });

  it('returns 0 for empty list', () => {
    expect(usedDaysByType([], 'ANNUAL')).toBe(0);
  });
});
