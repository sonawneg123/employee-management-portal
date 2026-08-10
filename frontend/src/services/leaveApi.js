/**
 * @fileoverview Leave Request API service.
 */

import axiosInstance from '@/api/axiosInstance';
import { API_ENDPOINTS } from '@/constants/api';

/**
 * @typedef {Object} LeaveRequestResponse
 * @property {string}      id
 * @property {string}      employeeId
 * @property {string}      leaveType
 * @property {string}      startDate
 * @property {string}      endDate
 * @property {string|null} reason
 * @property {string}      status
 * @property {string|null} reviewedBy
 * @property {string|null} reviewedAt
 * @property {string|null} [employeeName]  - Display name of the leave requester.
 * @property {string|null} [employeeCode]  - Employee code of the requester.
 * @property {string|null} [departmentName]- Department of the requester.
 * @property {number}      [totalDays]     - Calculated working days.
 * @property {boolean}     [isEmergency]   - Emergency leave flag.
 * @property {string|null} [attachmentUrl] - Optional supporting document URL.
 * @property {string}      createdAt
 * @property {string}      updatedAt
 */

/**
 * @typedef {Object} LeaveListParams
 * @property {number}  [page=0]
 * @property {number}  [size=20]
 * @property {string}  [sort='createdAt']
 * @property {string}  [direction='desc']
 * @property {string}  [status]
 * @property {string}  [type]
 * @property {string}  [employeeId]
 * @property {string}  [search]
 */

/**
 * Returns a paginated list of leave requests.
 *
 * @param {LeaveListParams} [params={}]
 * @returns {Promise<import('./employeeApi').PageResponse<LeaveRequestResponse>>}
 */
export async function getLeaveRequests(params = {}) {
  const { data } = await axiosInstance.get(API_ENDPOINTS.LEAVES, { params });
  return data;
}

/**
 * Returns the authenticated user's own leave requests (scoped server-side).
 * Calls GET /leaves/my which the backend automatically filters to the
 * current user's linked employee record — avoids the userId vs employeeId mismatch.
 *
 * @param {{ page?: number, size?: number, sort?: string, direction?: string, status?: string, type?: string }} [params={}]
 * @returns {Promise<import('./employeeApi').PageResponse<LeaveRequestResponse>>}
 */
export async function getMyLeaveRequests(params = {}) {
  const { data } = await axiosInstance.get(API_ENDPOINTS.LEAVES_MY, { params });
  return data;
}

/**
 * Returns a single leave request by UUID.
 *
 * @param {string} id
 * @returns {Promise<LeaveRequestResponse>}
 */
export async function getLeaveById(id) {
  const { data } = await axiosInstance.get(API_ENDPOINTS.LEAVE_BY_ID(id));
  return data;
}

/**
 * Submits a new leave request.
 *
 * @param {Object} payload
 * @returns {Promise<LeaveRequestResponse>}
 */
export async function createLeaveRequest(payload) {
  const { data } = await axiosInstance.post(API_ENDPOINTS.LEAVES, payload);
  return data;
}

/**
 * Approves a pending leave request (HR/Admin only).
 *
 * @param {string} id
 * @returns {Promise<LeaveRequestResponse>}
 */
export async function approveLeave(id) {
  const { data } = await axiosInstance.post(API_ENDPOINTS.LEAVE_APPROVE(id));
  return data;
}

/**
 * Rejects a pending leave request (HR/Admin only).
 *
 * @param {string} id
 * @param {{ reason?: string }} [payload={}]
 * @returns {Promise<LeaveRequestResponse>}
 */
export async function rejectLeave(id, payload = {}) {
  const { data } = await axiosInstance.post(API_ENDPOINTS.LEAVE_REJECT(id), payload);
  return data;
}

/**
 * Updates an existing leave request (only while still PENDING).
 *
 * @param {string} id
 * @param {Object} payload
 * @returns {Promise<LeaveRequestResponse>}
 */
export async function updateLeaveRequest(id, payload) {
  const { data } = await axiosInstance.put(API_ENDPOINTS.LEAVE_BY_ID(id), payload);
  return data;
}

/**
 * Cancels a leave request (employee own record only).
 *
 * @param {string} id
 * @returns {Promise<void>}
 */
export async function cancelLeave(id) {
  await axiosInstance.delete(API_ENDPOINTS.LEAVE_BY_ID(id));
}
