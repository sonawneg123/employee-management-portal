/**
 * @fileoverview Employee API service.
 *
 * Provides typed wrappers around all employee-related REST endpoints.
 */

import axiosInstance from '@/api/axiosInstance';
import { API_ENDPOINTS } from '@/constants/api';

/**
 * @typedef {Object} EmployeeResponse
 * @property {string}  id
 * @property {string}  employeeCode
 * @property {string}  departmentId
 * @property {string}  departmentName
 * @property {string|null} userId
 * @property {string|null} firstName
 * @property {string|null} lastName
 * @property {string|null} email
 * @property {string}  jobTitle
 * @property {string|null} phone
 * @property {string|null} address
 * @property {string}  dateOfJoining
 * @property {number}  salary
 * @property {string}  status
 * @property {string}  createdAt
 * @property {string}  updatedAt
 */

/**
 * @typedef {Object} PageResponse
 * @template T
 * @property {T[]}    content
 * @property {number} page
 * @property {number} size
 * @property {number} totalElements
 * @property {number} totalPages
 * @property {boolean} last
 */

/**
 * @typedef {Object} EmployeeListParams
 * @property {string}  [keyword]
 * @property {number}  [page=0]
 * @property {number}  [size=20]
 * @property {string}  [sortBy='createdAt']
 * @property {string}  [sortDir='desc']
 */

/**
 * Returns a paginated list of employees.
 *
 * @param {EmployeeListParams} [params={}] - Query parameters.
 * @returns {Promise<PageResponse<EmployeeResponse>>}
 */
export async function getEmployees(params = {}) {
  const { data } = await axiosInstance.get(API_ENDPOINTS.EMPLOYEES, { params });
  return data;
}

/**
 * Returns a single employee by UUID.
 *
 * @param {string} id - Employee UUID.
 * @returns {Promise<EmployeeResponse>}
 */
export async function getEmployeeById(id) {
  const { data } = await axiosInstance.get(API_ENDPOINTS.EMPLOYEE_BY_ID(id));
  return data;
}

/**
 * Creates a new employee record.
 *
 * @param {Object} payload - CreateEmployeeRequest payload.
 * @returns {Promise<EmployeeResponse>}
 */
export async function createEmployee(payload) {
  const { data } = await axiosInstance.post(API_ENDPOINTS.EMPLOYEES, payload);
  return data;
}

/**
 * Fully replaces an existing employee record.
 *
 * @param {string} id      - Employee UUID to update.
 * @param {Object} payload - UpdateEmployeeRequest payload.
 * @returns {Promise<EmployeeResponse>}
 */
export async function updateEmployee(id, payload) {
  const { data } = await axiosInstance.put(API_ENDPOINTS.EMPLOYEE_BY_ID(id), payload);
  return data;
}

/**
 * Deletes an employee by UUID.
 *
 * @param {string} id - Employee UUID to delete.
 * @returns {Promise<void>}
 */
export async function deleteEmployee(id) {
  await axiosInstance.delete(API_ENDPOINTS.EMPLOYEE_BY_ID(id));
}
