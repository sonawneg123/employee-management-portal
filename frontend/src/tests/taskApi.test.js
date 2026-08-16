/**
 * @fileoverview Tests for taskApi service functions.
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import axiosInstance from '@/api/axiosInstance';
import {
  getTasks,
  getMyTasks,
  getTaskById,
  createTask,
  updateTask,
  updateTaskStatus,
  deleteTask,
} from '@/services/taskApi';

vi.mock('@/api/axiosInstance');

const mockTask = {
  id: 'task-1',
  title: 'Test Task',
  status: 'ASSIGNED',
  priority: 'MEDIUM',
  overdue: false,
  dueDate: '2025-12-31',
  createdAt: '2025-01-01T00:00:00',
  updatedAt: '2025-01-01T00:00:00',
};

describe('taskApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('getTasks calls GET /tasks with params', async () => {
    axiosInstance.get = vi.fn().mockResolvedValue({ data: { content: [mockTask] } });
    const result = await getTasks({ page: 0, size: 20 });
    expect(axiosInstance.get).toHaveBeenCalledWith('/tasks', { params: { page: 0, size: 20 } });
    expect(result.content[0].id).toBe('task-1');
  });

  it('getMyTasks calls GET /tasks/my', async () => {
    axiosInstance.get = vi.fn().mockResolvedValue({ data: { content: [] } });
    await getMyTasks({ status: 'ASSIGNED' });
    expect(axiosInstance.get).toHaveBeenCalledWith('/tasks/my', { params: { status: 'ASSIGNED' } });
  });

  it('getTaskById calls GET /tasks/:id', async () => {
    axiosInstance.get = vi.fn().mockResolvedValue({ data: mockTask });
    const result = await getTaskById('task-1');
    expect(axiosInstance.get).toHaveBeenCalledWith('/tasks/task-1');
    expect(result.id).toBe('task-1');
  });

  it('createTask calls POST /tasks with payload', async () => {
    axiosInstance.post = vi.fn().mockResolvedValue({ data: mockTask });
    const payload = { title: 'New Task', dueDate: '2025-12-31', priority: 'MEDIUM' };
    const result = await createTask(payload);
    expect(axiosInstance.post).toHaveBeenCalledWith('/tasks', payload);
    expect(result.id).toBe('task-1');
  });

  it('updateTask calls PUT /tasks/:id with payload', async () => {
    axiosInstance.put = vi.fn().mockResolvedValue({ data: { ...mockTask, title: 'Updated' } });
    const result = await updateTask('task-1', { title: 'Updated', dueDate: '2025-12-31' });
    expect(axiosInstance.put).toHaveBeenCalledWith('/tasks/task-1', { title: 'Updated', dueDate: '2025-12-31' });
    expect(result.title).toBe('Updated');
  });

  it('updateTaskStatus calls PATCH /tasks/:id/status', async () => {
    axiosInstance.patch = vi.fn().mockResolvedValue({ data: { ...mockTask, status: 'IN_PROGRESS' } });
    const result = await updateTaskStatus('task-1', 'IN_PROGRESS');
    expect(axiosInstance.patch).toHaveBeenCalledWith('/tasks/task-1/status', { status: 'IN_PROGRESS' });
    expect(result.status).toBe('IN_PROGRESS');
  });

  it('deleteTask calls DELETE /tasks/:id', async () => {
    axiosInstance.delete = vi.fn().mockResolvedValue({});
    await deleteTask('task-1');
    expect(axiosInstance.delete).toHaveBeenCalledWith('/tasks/task-1');
  });
});
