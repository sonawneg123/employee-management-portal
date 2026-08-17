/**
 * @fileoverview Profile API service.
 *
 * Wraps the backend profile endpoints so any component can fetch and update
 * the authenticated user's own employee profile.
 *
 * Backend endpoints:
 *   GET    /api/profile              — returns the full profile for the logged-in user
 *   PUT    /api/profile/personal     — updates personal fields (phone, address)
 *   POST   /api/profile/photo        — upload / replace profile photo (multipart)
 *   GET    /api/profile/photo        — stream own profile photo
 *   DELETE /api/profile/photo        — remove own profile photo
 */

import axiosInstance from '@/api/axiosInstance';

const BASE = '/profile';

/**
 * @typedef {Object} ProfileResponse
 * @property {string}      userId
 * @property {string}      email
 * @property {string}      firstName
 * @property {string}      lastName
 * @property {string}      roles
 * @property {string|null} employeeId
 * @property {string|null} employeeCode
 * @property {string|null} departmentId
 * @property {string|null} departmentName
 * @property {string|null} jobTitle
 * @property {string|null} phone
 * @property {string|null} address
 * @property {string|null} dateOfJoining
 * @property {number|null} salary
 * @property {string|null} status
 * @property {string|null} profilePhotoUrl  - Relative URL e.g. "/api/profile/photo", or null.
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
 * @param {{ firstName?: string, lastName?: string, phone?: string, address?: string }} payload
 * @returns {Promise<ProfileResponse>}
 */
export async function updatePersonalInfo(payload) {
  const { data } = await axiosInstance.put(`${BASE}/personal`, payload);
  return data;
}

/**
 * Uploads or replaces the authenticated user's profile photo.
 *
 * @param {File} file - Image file (JPG, JPEG, PNG, or WEBP; max 5 MB).
 * @param {(progress: number) => void} [onProgress] - Optional upload progress callback (0–100).
 * @returns {Promise<ProfileResponse>} Updated profile including new photo URL.
 */
export async function uploadProfilePhoto(file, onProgress) {
  const formData = new FormData();
  formData.append('photo', file);
  const { data } = await axiosInstance.post(`${BASE}/photo`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: onProgress
      ? (e) => {
          if (e.total) onProgress(Math.round((e.loaded * 100) / e.total));
        }
      : undefined,
  });
  return data;
}

/**
 * Returns the URL to stream the authenticated user's own profile photo.
 * The URL includes a cache-bust parameter so browsers re-fetch after upload.
 *
 * @param {number|string} [cacheBust]
 * @returns {string}
 */
export function getProfilePhotoUrl(cacheBust) {
  const base = `${axiosInstance.defaults.baseURL ?? ''}/api${BASE}/photo`;
  return cacheBust !== undefined ? `${base}?v=${cacheBust}` : base;
}

/**
 * Returns the URL to stream a specific employee's profile photo.
 *
 * @param {string} employeeId
 * @param {number|string} [cacheBust]
 * @returns {string}
 */
export function getEmployeePhotoUrl(employeeId, cacheBust) {
  const base = `${axiosInstance.defaults.baseURL ?? ''}/api/employees/${employeeId}/profile-photo`;
  return cacheBust !== undefined ? `${base}?v=${cacheBust}` : base;
}

/**
 * Removes the authenticated user's profile photo.
 *
 * @returns {Promise<void>}
 */
export async function deleteProfilePhoto() {
  await axiosInstance.delete(`${BASE}/photo`);
}
