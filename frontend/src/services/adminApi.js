/**
 * @fileoverview Admin API service — user management operations.
 *
 * All endpoints require ROLE_ADMIN.
 */

import axiosInstance from '@/api/axiosInstance';

const BASE = '/admin/users';

/**
 * @typedef {Object} UserListResponse
 * @property {string}   id
 * @property {string}   email
 * @property {string}   firstName
 * @property {string}   lastName
 * @property {string[]} roles
 * @property {boolean}  isEnabled
 * @property {boolean}  isLocked
 * @property {string}   createdAt
 */

/**
 * Returns a paginated list of all user accounts.
 *
 * @param {{ page?: number, size?: number }} [params={}]
 * @returns {Promise<import('./employeeApi').PageResponse<UserListResponse>>}
 */
export async function getUsers(params = {}) {
  const { data } = await axiosInstance.get(BASE, { params });
  return data;
}

/**
 * Updates the role of a user account (replaces existing roles).
 *
 * @param {string} userId   UUID of the user
 * @param {string} roleName New role name (e.g. "ROLE_HR")
 * @returns {Promise<UserListResponse>}
 */
export async function updateUserRole(userId, roleName) {
  const { data } = await axiosInstance.put(`${BASE}/${userId}/role`, { roleName });
  return data;
}

/**
 * Enables or disables a user account.
 *
 * @param {string}  userId
 * @param {boolean} enabled
 * @returns {Promise<UserListResponse>}
 */
export async function setUserEnabled(userId, enabled) {
  const { data } = await axiosInstance.put(`${BASE}/${userId}/enabled`, null, {
    params: { enabled },
  });
  return data;
}
