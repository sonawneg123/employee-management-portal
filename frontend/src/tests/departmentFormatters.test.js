/**
 * @fileoverview Tests for department formatter utilities.
 *
 * Covers:
 *   - deptInitials / deptAvatarColor
 *   - formatEmployeeCount / formatDeptCode / formatHeadName
 *   - formatDeptCreatedAt
 *   - buildDeptCsvString / downloadDeptCsv
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import {
  deptInitials,
  deptAvatarColor,
  formatEmployeeCount,
  formatDeptCode,
  formatHeadName,
  buildDeptCsvString,
  downloadDeptCsv,
} from '@/utils/departmentFormatters';

// ── deptInitials ──────────────────────────────────────────────────────────────

describe('deptInitials', () => {
  it('returns first two chars of a single-word name', () => {
    expect(deptInitials('Engineering')).toBe('EN');
  });

  it('returns first chars of first two words', () => {
    expect(deptInitials('Human Resources')).toBe('HR');
    expect(deptInitials('Information Technology')).toBe('IT');
  });

  it('returns ? for null/undefined', () => {
    expect(deptInitials(null)).toBe('?');
    expect(deptInitials(undefined)).toBe('?');
  });

  it('returns ? for empty string', () => {
    expect(deptInitials('')).toBe('?');
  });
});

// ── deptAvatarColor ───────────────────────────────────────────────────────────

describe('deptAvatarColor', () => {
  it('returns a hex colour string', () => {
    expect(deptAvatarColor('Engineering')).toMatch(/^#[0-9a-f]{6}$/i);
  });

  it('is deterministic for the same name', () => {
    expect(deptAvatarColor('HR')).toBe(deptAvatarColor('HR'));
  });

  it('returns default colour for null', () => {
    expect(deptAvatarColor(null)).toBe('#1976d2');
  });
});

// ── formatEmployeeCount ───────────────────────────────────────────────────────

describe('formatEmployeeCount', () => {
  it('returns "N employees" for counts > 1', () => {
    expect(formatEmployeeCount(42)).toBe('42 employees');
  });

  it('returns "1 employee" for count of 1', () => {
    expect(formatEmployeeCount(1)).toBe('1 employee');
  });

  it('returns "0 employees" for zero', () => {
    expect(formatEmployeeCount(0)).toBe('0 employees');
  });

  it('returns — for null/undefined', () => {
    expect(formatEmployeeCount(null)).toBe('—');
    expect(formatEmployeeCount(undefined)).toBe('—');
  });
});

// ── formatDeptCode ────────────────────────────────────────────────────────────

describe('formatDeptCode', () => {
  it('returns uppercase code', () => {
    expect(formatDeptCode('eng')).toBe('ENG');
    expect(formatDeptCode('HR')).toBe('HR');
  });

  it('returns — for null', () => {
    expect(formatDeptCode(null)).toBe('—');
    expect(formatDeptCode(undefined)).toBe('—');
  });
});

// ── formatHeadName ────────────────────────────────────────────────────────────

describe('formatHeadName', () => {
  it('returns the name as-is', () => {
    expect(formatHeadName('Alice Johnson')).toBe('Alice Johnson');
  });

  it('returns — for empty/null', () => {
    expect(formatHeadName(null)).toBe('—');
    expect(formatHeadName('')).toBe('—');
    expect(formatHeadName('   ')).toBe('—');
  });
});

// ── buildDeptCsvString ────────────────────────────────────────────────────────

describe('buildDeptCsvString', () => {
  const headers = ['Name', 'Code', 'Head'];
  const fields = ['name', 'code', 'headName'];
  const depts = [
    { name: 'Engineering', code: 'ENG', headName: 'Alice' },
    { name: 'HR', code: 'HR', headName: null },
  ];

  it('creates a CSV with header row and data rows', () => {
    const csv = buildDeptCsvString(depts, headers, fields);
    const lines = csv.split('\r\n');
    expect(lines[0]).toBe('"Name","Code","Head"');
    expect(lines[1]).toBe('"Engineering","ENG","Alice"');
    expect(lines[2]).toBe('"HR","HR",""');
  });

  it('escapes double quotes in values', () => {
    const data = [{ name: 'A "Test" Dept', code: 'X', headName: '' }];
    const csv = buildDeptCsvString(data, headers, fields);
    expect(csv).toContain('"A ""Test"" Dept"');
  });
});

// ── downloadDeptCsv ───────────────────────────────────────────────────────────

describe('downloadDeptCsv', () => {
  let createObjectURL;
  let revokeObjectURL;
  let appendChildSpy;
  let removeChildSpy;
  let clickSpy;

  beforeEach(() => {
    createObjectURL = vi.fn().mockReturnValue('blob:mock');
    revokeObjectURL = vi.fn();
    global.URL.createObjectURL = createObjectURL;
    global.URL.revokeObjectURL = revokeObjectURL;

    clickSpy = vi.fn();
    appendChildSpy = vi.spyOn(document.body, 'appendChild').mockImplementation((el) => {
      el.click = clickSpy;
      return el;
    });
    removeChildSpy = vi.spyOn(document.body, 'removeChild').mockImplementation(() => {});
  });

  afterEach(() => {
    appendChildSpy.mockRestore();
    removeChildSpy.mockRestore();
  });

  it('creates an anchor element and clicks it', () => {
    downloadDeptCsv('"Name"\r\n"Engineering"', 'test.csv');
    expect(createObjectURL).toHaveBeenCalledOnce();
    expect(clickSpy).toHaveBeenCalledOnce();
    expect(revokeObjectURL).toHaveBeenCalledOnce();
  });
});
