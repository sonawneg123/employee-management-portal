/**
 * @fileoverview Task API service.
 */

import axiosInstance from '@/api/axiosInstance';
import { API_ENDPOINTS } from '@/constants/api';

/**
 * @typedef {Object} TaskResponse
 * @property {string}      id
 * @property {string}      title
 * @property {string|null} description
 * @property {string|null} guidelines
 * @property {string|null} acceptanceCriteria
 * @property {string|null} assignedEmployeeId
 * @property {string|null} assignedEmployeeName
 * @property {string|null} assignedEmployeeCode
 * @property {string|null} createdByEmployeeId
 * @property {string|null} createdByEmployeeName
 * @property {string}      priority
 * @property {string}      status
 * @property {boolean}     overdue
 * @property {string|null} dueDate
 * @property {number|null} estimatedHours
 * @property {string|null} category
 * @property {string}      createdAt
 * @property {string}      updatedAt
 * @property {string|null} createdBy
 * @property {string|null} updatedBy
 */

/**
 * @typedef {Object} TaskListParams
 * @property {number}  [page=0]
 * @property {number}  [size=20]
 * @property {string}  [sort='createdAt']
 * @property {string}  [direction='desc']
 * @property {string}  [status]
 * @property {string}  [priority]
 * @property {string}  [assignedEmployeeId]
 * @property {string}  [createdByEmployeeId]
 */

/**
 * Returns a paginated list of tasks.
 * EMPLOYEE callers are automatically scoped to their own tasks server-side.
 *
 * @param {TaskListParams} [params={}]
 * @returns {Promise<import('./employeeApi').PageResponse<TaskResponse>>}
 */
export async function getTasks(params = {}) {
  const { data } = await axiosInstance.get(API_ENDPOINTS.TASKS, { params });
  return data;
}

/**
 * Returns the authenticated user's assigned tasks.
 *
 * @param {TaskListParams} [params={}]
 * @returns {Promise<import('./employeeApi').PageResponse<TaskResponse>>}
 */
export async function getMyTasks(params = {}) {
  const { data } = await axiosInstance.get(API_ENDPOINTS.TASKS_MY, { params });
  return data;
}

/**
 * Returns tasks created by the authenticated user.
 *
 * @param {TaskListParams} [params={}]
 * @returns {Promise<import('./employeeApi').PageResponse<TaskResponse>>}
 */
export async function getCreatedTasks(params = {}) {
  const { data } = await axiosInstance.get(API_ENDPOINTS.TASKS_CREATED, { params });
  return data;
}

/**
 * Returns a single task by UUID.
 *
 * @param {string} id
 * @returns {Promise<TaskResponse>}
 */
export async function getTaskById(id) {
  const { data } = await axiosInstance.get(API_ENDPOINTS.TASK_BY_ID(id));
  return data;
}

/**
 * Creates a new task. Requires MANAGER, HR, or ADMIN role.
 *
 * @param {Object} payload
 * @returns {Promise<TaskResponse>}
 */
export async function createTask(payload) {
  const { data } = await axiosInstance.post(API_ENDPOINTS.TASKS, payload);
  return data;
}

/**
 * Fully updates an existing task. Requires MANAGER, HR, or ADMIN role.
 *
 * @param {string} id
 * @param {Object} payload
 * @returns {Promise<TaskResponse>}
 */
export async function updateTask(id, payload) {
  const { data } = await axiosInstance.put(API_ENDPOINTS.TASK_BY_ID(id), payload);
  return data;
}

/**
 * Updates only the status of a task.
 * EMPLOYEE callers are restricted to permitted transitions on their own tasks.
 *
 * @param {string} id
 * @param {string} status - The new TaskStatus value
 * @returns {Promise<TaskResponse>}
 */
export async function updateTaskStatus(id, status) {
  const { data } = await axiosInstance.patch(API_ENDPOINTS.TASK_STATUS(id), { status });
  return data;
}

/**
 * Deletes a task by UUID. Requires MANAGER, HR, or ADMIN role.
 *
 * @param {string} id
 * @returns {Promise<void>}
 */
export async function deleteTask(id) {
  await axiosInstance.delete(API_ENDPOINTS.TASK_BY_ID(id));
}

// ── Comments ─────────────────────────────────────────────────────────────────

/**
 * Returns all comments for a task.
 *
 * @param {string} taskId
 * @returns {Promise<Array>}
 */
export async function getTaskComments(taskId) {
  const { data } = await axiosInstance.get(API_ENDPOINTS.TASK_COMMENTS(taskId));
  return data;
}

/**
 * Posts a new comment on a task.
 *
 * @param {string} taskId
 * @param {string} content
 * @returns {Promise<Object>}
 */
export async function createTaskComment(taskId, content) {
  const { data } = await axiosInstance.post(API_ENDPOINTS.TASK_COMMENTS(taskId), { content });
  return data;
}

// ── Attachments ───────────────────────────────────────────────────────────────

/**
 * Returns all reference attachments for a task.
 *
 * @param {string} taskId
 * @returns {Promise<Array>}
 */
export async function getTaskAttachments(taskId) {
  const { data } = await axiosInstance.get(API_ENDPOINTS.TASK_ATTACHMENTS(taskId));
  return data;
}

/**
 * Uploads a reference attachment to a task. Requires MANAGER, HR, or ADMIN role.
 *
 * @param {string} taskId
 * @param {File} file
 * @returns {Promise<Object>}
 */
export async function uploadTaskAttachment(taskId, file) {
  const formData = new FormData();
  formData.append('file', file);
  const { data } = await axiosInstance.post(API_ENDPOINTS.TASK_ATTACHMENTS(taskId), formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  return data;
}

/**
 * Downloads an attachment as a blob and triggers a browser download.
 *
 * @param {string} taskId
 * @param {string} attachmentId
 * @param {string} filename
 * @returns {Promise<void>}
 */
export async function downloadTaskAttachment(taskId, attachmentId, filename) {
  const response = await axiosInstance.get(
    API_ENDPOINTS.TASK_ATTACHMENT_DOWNLOAD(taskId, attachmentId),
    { responseType: 'blob' },
  );
  const url = window.URL.createObjectURL(new Blob([response.data]));
  const link = document.createElement('a');
  link.href = url;
  link.setAttribute('download', filename);
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
}

/**
 * Deletes a reference attachment. Requires MANAGER, HR, or ADMIN role.
 *
 * @param {string} taskId
 * @param {string} attachmentId
 * @returns {Promise<void>}
 */
export async function deleteTaskAttachment(taskId, attachmentId) {
  await axiosInstance.delete(API_ENDPOINTS.TASK_ATTACHMENT_BY_ID(taskId, attachmentId));
}

// ── Activities ────────────────────────────────────────────────────────────────

/**
 * Returns the activity timeline for a task.
 *
 * @param {string} taskId
 * @returns {Promise<Array>}
 */
export async function getTaskActivities(taskId) {
  const { data } = await axiosInstance.get(API_ENDPOINTS.TASK_ACTIVITIES(taskId));
  return data;
}

// ── Reassignment ──────────────────────────────────────────────────────────────

/**
 * Reassigns a task to a different employee. Requires MANAGER, HR, or ADMIN role.
 *
 * @param {string} taskId
 * @param {string} newEmployeeId
 * @param {string} [reason]
 * @returns {Promise<Object>}
 */
export async function reassignTask(taskId, newEmployeeId, reason) {
  const { data } = await axiosInstance.post(API_ENDPOINTS.TASK_REASSIGN(taskId), {
    newEmployeeId,
    reason: reason || null,
  });
  return data;
}

// ── Availability & workload ───────────────────────────────────────────────────

/**
 * Returns employee availability and workload for assignment purposes.
 *
 * @returns {Promise<Array>}
 */
export async function getEmployeeAvailability() {
  const { data } = await axiosInstance.get(API_ENDPOINTS.TASK_EMPLOYEE_AVAILABILITY);
  return data;
}

/**
 * Returns server-side task dashboard statistics.
 *
 * @returns {Promise<Object>}
 */
export async function getTaskDashboardStats() {
  const { data } = await axiosInstance.get(API_ENDPOINTS.TASK_DASHBOARD_STATS);
  return data;
}

/**
 * Returns workload summary for all employees.
 *
 * @returns {Promise<Array>}
 */
export async function getTaskWorkloadSummary() {
  const { data } = await axiosInstance.get(API_ENDPOINTS.TASK_WORKLOAD_SUMMARY);
  return data;
}

/**
 * Returns workload info for a single employee.
 *
 * @param {string} employeeId
 * @returns {Promise<Object>}
 */
export async function getTaskWorkload(employeeId) {
  const { data } = await axiosInstance.get(API_ENDPOINTS.TASK_WORKLOAD(employeeId));
  return data;
}
