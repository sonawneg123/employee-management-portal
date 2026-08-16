/**
 * @fileoverview React Query hooks for the notification system.
 */

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  getNotifications,
  getUnreadCount,
  markNotificationRead,
  markAllNotificationsRead,
} from '@/services/notificationApi';

// ── Query key factories ──────────────────────────────────────────────────────

export const notificationKeys = {
  all: ['notifications'],
  list: (params) => [...notificationKeys.all, 'list', params],
  unreadCount: () => [...notificationKeys.all, 'unread-count'],
};

// ── Query hooks ──────────────────────────────────────────────────────────────

/**
 * Fetches the authenticated user's notifications.
 *
 * @param {Object} params
 * @param {{ enabled?: boolean, refetchInterval?: number }} [options]
 * @returns {import('@tanstack/react-query').UseQueryResult}
 */
export function useNotifications(params = {}, options = {}) {
  return useQuery({
    queryKey: notificationKeys.list(params),
    queryFn: () => getNotifications(params),
    staleTime: 30_000,
    ...options,
  });
}

/**
 * Fetches the unread notification count.
 * Polls every 15 seconds while the user is on an active page.
 *
 * @param {{ enabled?: boolean }} [options]
 * @returns {import('@tanstack/react-query').UseQueryResult}
 */
export function useUnreadCount(options = {}) {
  return useQuery({
    queryKey: notificationKeys.unreadCount(),
    queryFn: getUnreadCount,
    staleTime: 10_000,
    refetchInterval: 15_000,
    refetchIntervalInBackground: false,
    ...options,
  });
}

// ── Mutation hooks ───────────────────────────────────────────────────────────

/**
 * Marks a single notification as read.
 *
 * @returns {import('@tanstack/react-query').UseMutationResult}
 */
export function useMarkNotificationRead() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id) => markNotificationRead(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: notificationKeys.all });
    },
  });
}

/**
 * Marks all notifications as read.
 *
 * @returns {import('@tanstack/react-query').UseMutationResult}
 */
export function useMarkAllNotificationsRead() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: markAllNotificationsRead,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: notificationKeys.all });
    },
  });
}
