/**
 * @fileoverview Tests for employee formatter utilities.
 *
 * Covers:
 *   - formatFullName / formatInitials
 *   - formatSalary
 *   - formatPhone
 *   - formatStatusLabel
 *   - formatYearsOfService
 *   - avatarColorFromName
 *   - buildCsvString / downloadCsv
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import {
  formatFullName,
  formatInitials,
  formatSalary,
  formatPhone,
  formatStatusLabel,
  formatYearsOfService,
  avatarColorFromName,
  buildCsvString,
  downloadCsv,
} from '@/utils/employeeFormatters';

// ── formatFullName ─────────────────────────────────────────────────────────────

describe('formatFullName', () => {
  it('combines first and last name', () => {
    expect(formatFullName('Jane', 'Smith')).toBe('Jane Smith');
  });

  it('handles missing last name', () => {
    expect(formatFullName('Jane', null)).toBe('Jane');
  });

  it('handles missing first name', () => {
    expect(formatFullName(null, 'Smith')).toBe('Smith');
  });

  it('returns — when both are missing', () => {
    expect(formatFullName(null, null)).toBe('—');
    expect(formatFullName(undefined, undefined)).toBe('—');
  });
});

// ── formatInitials ─────────────────────────────────────────────────────────────

describe('formatInitials', () => {
  it('returns uppercase initials', () => {
    expect(formatInitials('jane', 'smith')).toBe('JS');
  });

  it('handles single name', () => {
    expect(formatInitials('Jane', null)).toBe('J');
    expect(formatInitials(null, 'Smith')).toBe('S');
  });

  it('returns ? when no name is provided', () => {
    expect(formatInitials(null, null)).toBe('?');
  });
});

// ── formatSalary ───────────────────────────────────────────────────────────────

describe('formatSalary', () => {
  it('formats a number as USD currency', () => {
    expect(formatSalary(75000)).toBe('$75,000.00');
  });

  it('returns — for null', () => {
    expect(formatSalary(null)).toBe('—');
    expect(formatSalary(undefined)).toBe('—');
  });

  it('formats zero correctly', () => {
    expect(formatSalary(0)).toBe('$0.00');
  });
});

// ── formatPhone ────────────────────────────────────────────────────────────────

describe('formatPhone', () => {
  it('returns the phone number as-is', () => {
    expect(formatPhone('+1-555-555-0000')).toBe('+1-555-555-0000');
  });

  it('returns — for empty/null', () => {
    expect(formatPhone(null)).toBe('—');
    expect(formatPhone('')).toBe('—');
    expect(formatPhone('   ')).toBe('—');
  });
});

// ── formatStatusLabel ──────────────────────────────────────────────────────────

describe('formatStatusLabel', () => {
  it('converts ON_LEAVE to On Leave', () => {
    expect(formatStatusLabel('ON_LEAVE')).toBe('On Leave');
  });

  it('converts ACTIVE to Active', () => {
    expect(formatStatusLabel('ACTIVE')).toBe('Active');
  });

  it('converts TERMINATED to Terminated', () => {
    expect(formatStatusLabel('TERMINATED')).toBe('Terminated');
  });

  it('returns — for null', () => {
    expect(formatStatusLabel(null)).toBe('—');
    expect(formatStatusLabel(undefined)).toBe('—');
  });
});

// ── avatarColorFromName ────────────────────────────────────────────────────────

describe('avatarColorFromName', () => {
  it('returns a hex colour string', () => {
    const color = avatarColorFromName('Alice');
    expect(color).toMatch(/^#[0-9a-f]{6}$/i);
  });

  it('returns the same colour for the same name', () => {
    expect(avatarColorFromName('Bob')).toBe(avatarColorFromName('Bob'));
  });

  it('returns a default colour for null/undefined', () => {
    expect(avatarColorFromName(null)).toBe('#1976d2');
  });
});

// ── buildCsvString ─────────────────────────────────────────────────────────────

describe('buildCsvString', () => {
  const headers = ['First Name', 'Last Name', 'Email'];
  const fields  = ['firstName', 'lastName', 'email'];
  const employees = [
    { firstName: 'Jane', lastName: 'Smith', email: 'jane@example.com' },
    { firstName: 'Bob',  lastName: 'Jones', email: 'bob@example.com'  },
  ];

  it('creates a CSV with header row and data rows', () => {
    const csv = buildCsvString(employees, headers, fields);
    const lines = csv.split('\r\n');
    expect(lines[0]).toBe('"First Name","Last Name","Email"');
    expect(lines[1]).toBe('"Jane","Smith","jane@example.com"');
    expect(lines[2]).toBe('"Bob","Jones","bob@example.com"');
  });

  it('escapes double quotes in values', () => {
    const data = [{ firstName: 'O"Brien', lastName: 'X', email: 'a@b.com' }];
    const csv  = buildCsvString(data, headers, fields);
    expect(csv).toContain('"O""Brien"');
  });

  it('handles null/undefined values as empty strings', () => {
    const data = [{ firstName: null, lastName: undefined, email: 'a@b.com' }];
    const csv  = buildCsvString(data, headers, fields);
    expect(csv).toContain('"","",');
  });
});

// ── downloadCsv ────────────────────────────────────────────────────────────────

describe('downloadCsv', () => {
  let createObjectURL;
  let revokeObjectURL;
  let appendChildSpy;
  let removeChildSpy;
  let clickSpy;

  beforeEach(() => {
    createObjectURL  = vi.fn().mockReturnValue('blob:mock');
    revokeObjectURL  = vi.fn();
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
    downloadCsv('col1,col2\r\n1,2', 'test.csv');
    expect(createObjectURL).toHaveBeenCalledOnce();
    expect(clickSpy).toHaveBeenCalledOnce();
    expect(revokeObjectURL).toHaveBeenCalledOnce();
  });
});
