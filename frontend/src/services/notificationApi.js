/**
 * @fileoverview Notification API service.
 */

import axiosInstance from '@/api/axiosInstance';
import { API_ENDPOINTS } from '@/constants/api';

const BASE = API_ENDPOINTS.NOTIFICATIONS;

/**
 * @typedef {Object} NotificationResponse
 * @property {string}      id
 * @property {string}      type
 * @property {string}      title
 * @property {string}      message
 * @property {string|null} relatedTaskId
 * @property {boolean}     read
 * @property {string}      createdAt
 */

/**
 * Returns a paginated list of notifications for the authenticated user.
 *
 * @param {{ page?: number, size?: number }} [params={}]
 * @returns {Promise<import('./employeeApi').PageResponse<NotificationResponse>>}
 */
export async function getNotifications(params = {}) {
  const { data } = await axiosInstance.get(BASE, { params });
  return data;
}

/**
 * Returns the unread notification count for the authenticated user.
 *
 * @returns {Promise<{ unreadCount: number }>}
 */
export async function getUnreadCount() {
  const { data } = await axiosInstance.get(`${BASE}/unread-count`);
  return data;
}

/**
 * Marks a single notification as read.
 *
 * @param {string} id
 * @returns {Promise<void>}
 */
export async function markNotificationRead(id) {
  await axiosInstance.patch(`${BASE}/${id}/read`);
}

/**
 * Marks all notifications for the authenticated user as read.
 *
 * @returns {Promise<void>}
 */
export async function markAllNotificationsRead() {
  await axiosInstance.patch(`${BASE}/read-all`);
}
