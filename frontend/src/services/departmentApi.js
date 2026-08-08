/**
 * @fileoverview Department API service.
 *
 * Provides typed wrappers around all department REST endpoints.
 * Two list functions are exposed:
 *   - {@link getDepartments}        — simple flat list for dropdowns (no pagination).
 *   - {@link getDepartmentsPaged}   — paginated, filtered list for the management table.
 */

import axiosInstance from '@/api/axiosInstance';
import { API_ENDPOINTS } from '@/constants/api';

/**
 * @typedef {Object} DepartmentResponse
 * @property {string}         id
 * @property {string}         name
 * @property {string}         code
 * @property {string|null}    [description]
 * @property {string|null}    [headName]      - Display name of the department head.
 * @property {number}         [employeeCount] - Number of active employees.
 * @property {string}         createdAt
 * @property {string}         updatedAt
 */

/**
 * @typedef {Object} DepartmentPageResponse
 * @property {DepartmentResponse[]} content
 * @property {number} page
 * @property {number} size
 * @property {number} totalElements
 * @property {number} totalPages
 * @property {boolean} last
 */

/**
 * @typedef {Object} DepartmentListParams
 * @property {string}  [keyword]
 * @property {number}  [page=0]
 * @property {number}  [size=20]
 * @property {string}  [sort='name']
 * @property {string}  [direction='asc']
 */

/**
 * Returns all departments as a flat array (no pagination).
 * Used for form Autocomplete and filter dropdowns.
 * Calls GET /departments/all which returns a plain DepartmentResponse[].
 *
 * @returns {Promise<DepartmentResponse[]>}
 */
export async function getDepartments() {
  const { data } = await axiosInstance.get(`${API_ENDPOINTS.DEPARTMENTS}/all`);
  return data;
}

/**
 * Returns a paginated, filtered list of departments for the management table.
 *
 * @param {DepartmentListParams} [params={}]
 * @returns {Promise<DepartmentPageResponse>}
 */
export async function getDepartmentsPaged(params = {}) {
  const { data } = await axiosInstance.get(API_ENDPOINTS.DEPARTMENTS, { params });
  return data;
}

/**
 * Returns a single department by UUID.
 *
 * @param {string} id - Department UUID.
 * @returns {Promise<DepartmentResponse>}
 */
export async function getDepartmentById(id) {
  const { data } = await axiosInstance.get(API_ENDPOINTS.DEPARTMENT_BY_ID(id));
  return data;
}

/**
 * Creates a new department.
 *
 * @param {{ name: string, code: string }} payload
 * @returns {Promise<DepartmentResponse>}
 */
export async function createDepartment(payload) {
  const { data } = await axiosInstance.post(API_ENDPOINTS.DEPARTMENTS, payload);
  return data;
}

/**
 * Updates an existing department.
 *
 * @param {string} id
 * @param {{ name: string, code: string }} payload
 * @returns {Promise<DepartmentResponse>}
 */
export async function updateDepartment(id, payload) {
  const { data } = await axiosInstance.put(API_ENDPOINTS.DEPARTMENT_BY_ID(id), payload);
  return data;
}

/**
 * Deletes a department by UUID.
 *
 * @param {string} id
 * @returns {Promise<void>}
 */
export async function deleteDepartment(id) {
  await axiosInstance.delete(API_ENDPOINTS.DEPARTMENT_BY_ID(id));
}
