/**
 * @fileoverview Tests for notificationApi service functions.
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import axiosInstance from '@/api/axiosInstance';
import {
  getNotifications,
  getUnreadCount,
  markNotificationRead,
  markAllNotificationsRead,
} from '@/services/notificationApi';

vi.mock('@/api/axiosInstance');

const mockNotification = {
  id: 'notif-1',
  type: 'TASK_ASSIGNED',
  title: 'New Task Assigned',
  message: 'You have been assigned a task.',
  relatedTaskId: 'task-1',
  read: false,
  createdAt: '2025-01-01T09:00:00',
};

describe('notificationApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('getNotifications calls GET /notifications with params', async () => {
    axiosInstance.get = vi.fn().mockResolvedValue({ data: { content: [mockNotification] } });
    const result = await getNotifications({ page: 0, size: 20 });
    expect(axiosInstance.get).toHaveBeenCalledWith('/notifications', { params: { page: 0, size: 20 } });
    expect(result.content[0].id).toBe('notif-1');
  });

  it('getUnreadCount calls GET /notifications/unread-count', async () => {
    axiosInstance.get = vi.fn().mockResolvedValue({ data: { unreadCount: 3 } });
    const result = await getUnreadCount();
    expect(axiosInstance.get).toHaveBeenCalledWith('/notifications/unread-count');
    expect(result.unreadCount).toBe(3);
  });

  it('markNotificationRead calls PATCH /notifications/{id}/read', async () => {
    axiosInstance.patch = vi.fn().mockResolvedValue({});
    await markNotificationRead('notif-1');
    expect(axiosInstance.patch).toHaveBeenCalledWith('/notifications/notif-1/read');
  });

  it('markAllNotificationsRead calls PATCH /notifications/read-all', async () => {
    axiosInstance.patch = vi.fn().mockResolvedValue({});
    await markAllNotificationsRead();
    expect(axiosInstance.patch).toHaveBeenCalledWith('/notifications/read-all');
  });
});
