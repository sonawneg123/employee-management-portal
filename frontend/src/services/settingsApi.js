/**
 * settingsApi.js
 * API client for Settings endpoints.
 */
import axiosInstance from '@/api/axiosInstance';

const BASE = '/settings';

/**
 * POST /settings/change-password
 * @param {{ currentPassword: string, newPassword: string, confirmPassword: string }} data
 * @returns {Promise<void>} resolves on 204 No Content
 */
export const changePassword = (data) =>
  axiosInstance.post(`${BASE}/change-password`, data);
