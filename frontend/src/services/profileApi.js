/**
 * @fileoverview Profile API service.
 *
 * Wraps the backend profile endpoints so any component can fetch and update
 * the authenticated user's own employee profile.
 *
 * Backend endpoints:
 *   GET /api/profile              — returns the full profile for the logged-in user
 *   PUT /api/profile/personal     — updates personal fields (phone, address)
 */

import axiosInstance from '@/api/axiosInstance';

const BASE = '/profile';

/**
 * @typedef {Object} ProfileResponse
 * @property {string}      id            - Employee UUID.
 * @property {string}      userId        - Linked User UUID.
 * @property {string}      firstName
 * @property {string}      lastName
 * @property {string}      email
 * @property {string|null} phone
 * @property {string|null} address
 * @property {string|null} jobTitle
 * @property {string|null} departmentId
 * @property {string|null} departmentName
 * @property {string|null} employeeCode
 * @property {string}      status
 * @property {string|null} dateOfJoining
 * @property {string}      createdAt
 * @property {string}      updatedAt
 */

/**
 * Fetches the authenticated user's own employee profile.
 *
 * @returns {Promise<ProfileResponse>}
 */
export async function getProfile() {
  const { data } = await axiosInstance.get(BASE);
  return data;
}

/**
 * Updates the authenticated user's personal information.
 *
 * @param {{ phone?: string, address?: string }} payload
 * @returns {Promise<ProfileResponse>}
 */
export async function updatePersonalInfo(payload) {
  const { data } = await axiosInstance.put(`${BASE}/personal`, payload);
  return data;
}
