/**
 * @fileoverview Date formatting and manipulation utilities using Day.js.
 *
 * All date operations in the application should go through this module
 * so that the date library can be swapped without touching component code.
 */

import dayjs from 'dayjs';
import relativeTime from 'dayjs/plugin/relativeTime';
import localizedFormat from 'dayjs/plugin/localizedFormat';
import utc from 'dayjs/plugin/utc';
import timezone from 'dayjs/plugin/timezone';

dayjs.extend(relativeTime);
dayjs.extend(localizedFormat);
dayjs.extend(utc);
dayjs.extend(timezone);

/**
 * Standard display format used across the application for date-only values.
 *
 * @type {string}
 */
export const DATE_FORMAT = 'MMM DD, YYYY';

/**
 * Standard display format for date-time values.
 *
 * @type {string}
 */
export const DATETIME_FORMAT = 'MMM DD, YYYY HH:mm';

/**
 * ISO-8601 date string format used when sending dates to the backend.
 *
 * @type {string}
 */
export const ISO_DATE_FORMAT = 'YYYY-MM-DD';

/**
 * Formats a date value for display.
 *
 * @param {string | Date | dayjs.Dayjs | null | undefined} date - The date to format.
 * @param {string} [format=DATE_FORMAT] - Day.js format string.
 * @returns {string} The formatted date string, or {@code '—'} if the input is falsy.
 */
export function formatDate(date, format = DATE_FORMAT) {
  if (!date) return '—';
  return dayjs(date).format(format);
}

/**
 * Formats a date-time value for display.
 *
 * @param {string | Date | dayjs.Dayjs | null | undefined} datetime - The datetime to format.
 * @returns {string} The formatted datetime string, or {@code '—'} if the input is falsy.
 */
export function formatDateTime(datetime) {
  return formatDate(datetime, DATETIME_FORMAT);
}

/**
 * Returns a human-readable relative time string (e.g., "3 days ago").
 *
 * @param {string | Date | dayjs.Dayjs | null | undefined} date - The date to compare against now.
 * @returns {string} The relative time string, or {@code '—'} if the input is falsy.
 */
export function formatRelative(date) {
  if (!date) return '—';
  return dayjs(date).fromNow();
}

/**
 * Converts a date to ISO-8601 format (YYYY-MM-DD) for API submissions.
 *
 * @param {string | Date | dayjs.Dayjs | null | undefined} date - The date to convert.
 * @returns {string | null} The ISO date string, or {@code null}.
 */
export function toIsoDate(date) {
  if (!date) return null;
  return dayjs(date).format(ISO_DATE_FORMAT);
}

/**
 * Returns the number of calendar days between two dates (inclusive).
 *
 * @param {string | Date} startDate - The start date.
 * @param {string | Date} endDate   - The end date.
 * @returns {number} Number of days.
 */
export function daysBetween(startDate, endDate) {
  return dayjs(endDate).diff(dayjs(startDate), 'day') + 1;
}

/**
 * Returns whether the given date string is a valid calendar date.
 *
 * @param {string} dateString - The date string to validate.
 * @returns {boolean} {@code true} if valid.
 */
export function isValidDate(dateString) {
  return dayjs(dateString).isValid();
}
