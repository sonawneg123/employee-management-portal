/**
 * @fileoverview Attendance API service.
 */

import axiosInstance from '@/api/axiosInstance';
import { API_ENDPOINTS } from '@/constants/api';

/**
 * @typedef {Object} AttendanceResponse
 * @property {string}      id
 * @property {string}      employeeId
 * @property {string}      attendanceDate
 * @property {string|null} checkInTime
 * @property {string|null} checkOutTime
 * @property {string}      status
 * @property {string|null} notes
 * @property {string}      createdAt
 * @property {string}      updatedAt
 */

/**
 * @typedef {Object} AttendanceListParams
 * @property {string}  [employeeId]
 * @property {string}  [startDate]
 * @property {string}  [endDate]
 * @property {number}  [page=0]
 * @property {number}  [size=20]
 */

/**
 * Returns a paginated list of attendance records.
 *
 * @param {AttendanceListParams} [params={}]
 * @returns {Promise<import('./employeeApi').PageResponse<AttendanceResponse>>}
 */
export async function getAttendance(params = {}) {
  const { data } = await axiosInstance.get(API_ENDPOINTS.ATTENDANCE, { params });
  return data;
}

/**
 * Returns the authenticated employee's own attendance records.
 * Calls GET /attendance/my which the backend scopes to the current user.
 *
 * @param {{ startDate?: string, endDate?: string, page?: number, size?: number }} [params={}]
 * @returns {Promise<import('./employeeApi').PageResponse<AttendanceResponse>>}
 */
export async function getMyAttendance(params = {}) {
  const { data } = await axiosInstance.get(`${API_ENDPOINTS.ATTENDANCE}/my`, { params });
  return data;
}

/**
 * Returns a single attendance record by UUID.
 *
 * @param {string} id
 * @returns {Promise<AttendanceResponse>}
 */
export async function getAttendanceById(id) {
  const { data } = await axiosInstance.get(API_ENDPOINTS.ATTENDANCE_BY_ID(id));
  return data;
}

/**
 * Creates a new attendance record.
 *
 * @param {Object} payload
 * @returns {Promise<AttendanceResponse>}
 */
export async function createAttendance(payload) {
  const { data } = await axiosInstance.post(API_ENDPOINTS.ATTENDANCE, payload);
  return data;
}

/**
 * Updates an existing attendance record.
 *
 * @param {string} id
 * @param {Object} payload
 * @returns {Promise<AttendanceResponse>}
 */
export async function updateAttendance(id, payload) {
  const { data } = await axiosInstance.put(API_ENDPOINTS.ATTENDANCE_BY_ID(id), payload);
  return data;
}

/**
 * Deletes an attendance record.
 *
 * @param {string} id
 * @returns {Promise<void>}
 */
export async function deleteAttendance(id) {
  await axiosInstance.delete(API_ENDPOINTS.ATTENDANCE_BY_ID(id));
}
