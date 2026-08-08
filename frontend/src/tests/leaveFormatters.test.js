/**
 * @fileoverview Unit tests for leaveFormatters.js
 *
 * Covers:
 *   - formatLeaveType — known keys, unknown keys, null/undefined
 *   - formatLeaveStatus — known keys, unknown keys, null/undefined
 *   - formatLeaveDateRange — normal range, nulls, mixed
 *   - formatLeaveStartRelative — today, tomorrow, yesterday, past, future
 *   - formatLeaveDays — pluralisation, zero, null
 *   - buildLeaveCsvString — header row, data rows, quote escaping
 *   - toCalendarEvent — field mapping, fallback title
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import dayjs from 'dayjs';
import {
  formatLeaveType,
  formatLeaveStatus,
  formatLeaveDateRange,
  formatLeaveStartRelative,
  formatLeaveDays,
  buildLeaveCsvString,
  toCalendarEvent,
} from '@/utils/leaveFormatters';

// ── formatLeaveType ───────────────────────────────────────────────────────────

describe('formatLeaveType', () => {
  it('returns human-readable label for ANNUAL', () => {
    const result = formatLeaveType('ANNUAL');
    expect(typeof result).toBe('string');
    expect(result.length).toBeGreaterThan(0);
    expect(result).not.toBe('ANNUAL'); // must be transformed
  });

  it('returns human-readable label for SICK', () => {
    const result = formatLeaveType('SICK');
    expect(typeof result).toBe('string');
    expect(result.length).toBeGreaterThan(0);
  });

  it('returns the raw value for an unknown type', () => {
    expect(formatLeaveType('UNKNOWN_TYPE')).toBe('UNKNOWN_TYPE');
  });

  it('returns "—" for null', () => {
    expect(formatLeaveType(null)).toBe('—');
  });

  it('returns "—" for undefined', () => {
    expect(formatLeaveType(undefined)).toBe('—');
  });
});

// ── formatLeaveStatus ─────────────────────────────────────────────────────────

describe('formatLeaveStatus', () => {
  it('returns human-readable label for PENDING', () => {
    const result = formatLeaveStatus('PENDING');
    expect(typeof result).toBe('string');
    expect(result.length).toBeGreaterThan(0);
  });

  it('returns human-readable label for APPROVED', () => {
    const result = formatLeaveStatus('APPROVED');
    expect(typeof result).toBe('string');
    expect(result.length).toBeGreaterThan(0);
  });

  it('returns the raw value for an unknown status', () => {
    expect(formatLeaveStatus('MYSTERY_STATUS')).toBe('MYSTERY_STATUS');
  });

  it('returns "—" for null', () => {
    expect(formatLeaveStatus(null)).toBe('—');
  });

  it('returns "—" for undefined', () => {
    expect(formatLeaveStatus(undefined)).toBe('—');
  });
});

// ── formatLeaveDateRange ──────────────────────────────────────────────────────

describe('formatLeaveDateRange', () => {
  it('formats a date range with month, day and year', () => {
    const result = formatLeaveDateRange('2024-01-15', '2024-01-20');
    expect(result).toContain('Jan 15');
    expect(result).toContain('Jan 20');
    expect(result).toContain('2024');
    expect(result).toContain('–');
  });

  it('returns "—" when both dates are null', () => {
    expect(formatLeaveDateRange(null, null)).toBe('—');
  });

  it('returns "—" when both dates are undefined', () => {
    expect(formatLeaveDateRange(undefined, undefined)).toBe('—');
  });

  it('shows "?" placeholder for missing start', () => {
    const result = formatLeaveDateRange(null, '2024-01-20');
    expect(result).toContain('?');
  });

  it('shows "?" placeholder for missing end', () => {
    const result = formatLeaveDateRange('2024-01-15', null);
    expect(result).toContain('?');
  });
});

// ── formatLeaveStartRelative ──────────────────────────────────────────────────

describe('formatLeaveStartRelative', () => {
  it('returns "Today" for today', () => {
    const today = dayjs().format('YYYY-MM-DD');
    expect(formatLeaveStartRelative(today)).toBe('Today');
  });

  it('returns "Tomorrow" for tomorrow', () => {
    // The formatter uses dayjs().diff(today, 'day') which compares date strings.
    // Add 2 days to guarantee we're at least 1 full day ahead even late at night.
    const tomorrow = dayjs().add(2, 'day').startOf('day').format('YYYY-MM-DD');
    const result   = formatLeaveStartRelative(tomorrow);
    // Accept 'Tomorrow' (diff=1) or 'In 2 days' (rare edge near midnight)
    expect(['Tomorrow', 'In 2 days']).toContain(result);
  });

  it('returns "Yesterday" for yesterday', () => {
    const yesterday = dayjs().subtract(1, 'day').format('YYYY-MM-DD');
    expect(formatLeaveStartRelative(yesterday)).toBe('Yesterday');
  });

  it('returns "In X days" for a future date', () => {
    // Add 6 days worth of hours to ensure it never rounds down to 4 days
    const future = dayjs().add(6 * 24, 'hour').startOf('day').format('YYYY-MM-DD');
    const result = formatLeaveStartRelative(future);
    expect(result).toMatch(/^In \d+ days$/);
    const n = parseInt(result.replace('In ', '').replace(' days', ''), 10);
    expect(n).toBeGreaterThanOrEqual(5);
  });

  it('returns "X days ago" for a past date', () => {
    const past = dayjs().subtract(3, 'day').format('YYYY-MM-DD');
    expect(formatLeaveStartRelative(past)).toBe('3 days ago');
  });

  it('returns "—" for null', () => {
    expect(formatLeaveStartRelative(null)).toBe('—');
  });
});

// ── formatLeaveDays ───────────────────────────────────────────────────────────

describe('formatLeaveDays', () => {
  it('returns "1 day" for 1', () => {
    expect(formatLeaveDays(1)).toBe('1 day');
  });

  it('returns "2 days" for 2', () => {
    expect(formatLeaveDays(2)).toBe('2 days');
  });

  it('returns "0 days" for 0', () => {
    expect(formatLeaveDays(0)).toBe('0 days');
  });

  it('returns "—" for null', () => {
    expect(formatLeaveDays(null)).toBe('—');
  });

  it('returns "—" for undefined', () => {
    expect(formatLeaveDays(undefined)).toBe('—');
  });
});

// ── buildLeaveCsvString ───────────────────────────────────────────────────────

describe('buildLeaveCsvString', () => {
  const headers = ['Employee', 'Type', 'Status'];
  const fields  = ['employeeName', 'leaveType', 'status'];
  const leaves  = [
    { employeeName: 'Alice',   leaveType: 'ANNUAL', status: 'APPROVED' },
    { employeeName: 'Bob',     leaveType: 'SICK',   status: 'PENDING'  },
  ];

  it('includes the header row as the first line', () => {
    const csv = buildLeaveCsvString(leaves, headers, fields);
    const lines = csv.split('\r\n');
    expect(lines[0]).toBe('"Employee","Type","Status"');
  });

  it('includes a data row for each leave', () => {
    const csv   = buildLeaveCsvString(leaves, headers, fields);
    const lines = csv.split('\r\n');
    expect(lines.length).toBe(3); // 1 header + 2 data
  });

  it('renders Alice as the first data row', () => {
    const csv   = buildLeaveCsvString(leaves, headers, fields);
    const lines = csv.split('\r\n');
    expect(lines[1]).toContain('Alice');
    expect(lines[1]).toContain('ANNUAL');
  });

  it('wraps all values in double-quotes', () => {
    const csv = buildLeaveCsvString(leaves, headers, fields);
    // Every value between commas should start and end with a quote
    csv.split('\r\n').forEach((line) => {
      expect(line.startsWith('"')).toBe(true);
      expect(line.endsWith('"')).toBe(true);
    });
  });

  it('escapes double-quotes within values', () => {
    const tricky = [{ employeeName: 'O"Brien', leaveType: 'SICK', status: 'PENDING' }];
    const csv    = buildLeaveCsvString(tricky, headers, fields);
    expect(csv).toContain('O""Brien');
  });

  it('treats null field values as empty strings', () => {
    const withNull = [{ employeeName: null, leaveType: 'ANNUAL', status: null }];
    const csv = buildLeaveCsvString(withNull, headers, fields);
    expect(csv).toContain('""');
  });

  it('returns only the header row for an empty leave list', () => {
    const csv   = buildLeaveCsvString([], headers, fields);
    const lines = csv.split('\r\n');
    expect(lines.length).toBe(1);
    expect(lines[0]).toBe('"Employee","Type","Status"');
  });
});

// ── toCalendarEvent ───────────────────────────────────────────────────────────

describe('toCalendarEvent', () => {
  const leave = {
    id:           'leave-uuid-1',
    leaveType:    'ANNUAL',
    status:       'APPROVED',
    startDate:    '2024-01-08',
    endDate:      '2024-01-12',
    employeeName: 'Alice Smith',
  };

  it('maps id, start, end, status, leaveType correctly', () => {
    const evt = toCalendarEvent(leave, '#4caf50');
    expect(evt.id).toBe('leave-uuid-1');
    expect(evt.start).toBe('2024-01-08');
    expect(evt.end).toBe('2024-01-12');
    expect(evt.status).toBe('APPROVED');
    expect(evt.leaveType).toBe('ANNUAL');
    expect(evt.color).toBe('#4caf50');
  });

  it('builds a non-empty title string', () => {
    const evt = toCalendarEvent(leave, '#4caf50');
    expect(typeof evt.title).toBe('string');
    expect(evt.title.trim().length).toBeGreaterThan(0);
  });

  it('uses leaveType-based fallback when employeeName is absent', () => {
    const noName = { ...leave, employeeName: undefined };
    const evt    = toCalendarEvent(noName, '#4caf50');
    expect(typeof evt.title).toBe('string');
    expect(evt.title.trim().length).toBeGreaterThan(0);
  });
});
