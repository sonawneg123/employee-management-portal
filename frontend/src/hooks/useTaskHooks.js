/**
 * @fileoverview React Query hooks for Task management.
 *
 * Phase 6A.1 enhancements:
 *  - Manager task list uses a 20-second polling interval for live status updates.
 *  - Status mutations invalidate all task query variants so managers see
 *    employee-started tasks immediately on their next refetch cycle.
 * Phase 6C-6E enhancements:
 *  - Comments, attachments, activities, reassign, availability, workload hooks.
 */

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  getTasks,
  getMyTasks,
  getCreatedTasks,
  getTaskById,
  createTask,
  updateTask,
  updateTaskStatus,
  deleteTask,
  getTaskComments,
  createTaskComment,
  getTaskAttachments,
  uploadTaskAttachment,
  deleteTaskAttachment,
  getTaskActivities,
  reassignTask,
  getEmployeeAvailability,
  getTaskDashboardStats,
  getTaskWorkloadSummary,
  getTaskWorkload,
} from '@/services/taskApi';

// ── Query key factories ──────────────────────────────────────────────────────

export const taskKeys = {
  all: ['tasks'],
  lists: () => [...taskKeys.all, 'list'],
  list: (params) => [...taskKeys.lists(), params],
  myList: (params) => [...taskKeys.all, 'my', params],
  createdList: (params) => [...taskKeys.all, 'created', params],
  detail: (id) => [...taskKeys.all, 'detail', id],
  comments: (taskId) => [...taskKeys.all, 'comments', taskId],
  attachments: (taskId) => [...taskKeys.all, 'attachments', taskId],
  activities: (taskId) => [...taskKeys.all, 'activities', taskId],
  availability: () => [...taskKeys.all, 'availability'],
  dashboardStats: () => [...taskKeys.all, 'dashboard-stats'],
  workloadSummary: () => [...taskKeys.all, 'workload-summary'],
  workload: (employeeId) => [...taskKeys.all, 'workload', employeeId],
};

// ── Query hooks ──────────────────────────────────────────────────────────────

/**
 * Fetches a paginated list of tasks (manager/admin view).
 * Polls every 20 seconds while the page is visible.
 *
 * @param {Object} params
 * @returns {import('@tanstack/react-query').UseQueryResult}
 */
export function useTasks(params = {}) {
  return useQuery({
    queryKey: taskKeys.list(params),
    queryFn: () => getTasks(params),
    staleTime: 15_000,
    refetchInterval: 20_000,
    refetchIntervalInBackground: false,
  });
}

/**
 * Fetches the authenticated user's assigned tasks.
 *
 * @param {Object} params
 * @returns {import('@tanstack/react-query').UseQueryResult}
 */
export function useMyTasks(params = {}) {
  return useQuery({
    queryKey: taskKeys.myList(params),
    queryFn: () => getMyTasks(params),
    staleTime: 30_000,
  });
}

/**
 * Fetches tasks created by the authenticated user.
 *
 * @param {Object} params
 * @returns {import('@tanstack/react-query').UseQueryResult}
 */
export function useCreatedTasks(params = {}) {
  return useQuery({
    queryKey: taskKeys.createdList(params),
    queryFn: () => getCreatedTasks(params),
    staleTime: 30_000,
  });
}

/**
 * Fetches a single task by ID.
 * Uses a 20-second polling interval so the detail page stays fresh.
 *
 * @param {string} id
 * @returns {import('@tanstack/react-query').UseQueryResult}
 */
export function useTask(id) {
  return useQuery({
    queryKey: taskKeys.detail(id),
    queryFn: () => getTaskById(id),
    enabled: Boolean(id),
    staleTime: 15_000,
    refetchInterval: 20_000,
    refetchIntervalInBackground: false,
  });
}

/**
 * Fetches comments for a task. Polls every 20 seconds.
 *
 * @param {string} taskId
 * @returns {import('@tanstack/react-query').UseQueryResult}
 */
export function useTaskComments(taskId) {
  return useQuery({
    queryKey: taskKeys.comments(taskId),
    queryFn: () => getTaskComments(taskId),
    enabled: Boolean(taskId),
    staleTime: 15_000,
    refetchInterval: 20_000,
    refetchIntervalInBackground: false,
  });
}

/**
 * Fetches attachments for a task.
 *
 * @param {string} taskId
 * @returns {import('@tanstack/react-query').UseQueryResult}
 */
export function useTaskAttachments(taskId) {
  return useQuery({
    queryKey: taskKeys.attachments(taskId),
    queryFn: () => getTaskAttachments(taskId),
    enabled: Boolean(taskId),
    staleTime: 30_000,
  });
}

/**
 * Fetches the activity timeline for a task. Polls every 20 seconds.
 *
 * @param {string} taskId
 * @returns {import('@tanstack/react-query').UseQueryResult}
 */
export function useTaskActivities(taskId) {
  return useQuery({
    queryKey: taskKeys.activities(taskId),
    queryFn: () => getTaskActivities(taskId),
    enabled: Boolean(taskId),
    staleTime: 15_000,
    refetchInterval: 20_000,
    refetchIntervalInBackground: false,
  });
}

/**
 * Fetches employee availability with workload info. Polls every 30 seconds.
 *
 * @returns {import('@tanstack/react-query').UseQueryResult}
 */
export function useEmployeeAvailability() {
  return useQuery({
    queryKey: taskKeys.availability(),
    queryFn: getEmployeeAvailability,
    staleTime: 25_000,
    refetchInterval: 30_000,
    refetchIntervalInBackground: false,
  });
}

/**
 * Fetches server-side task dashboard statistics.
 *
 * @returns {import('@tanstack/react-query').UseQueryResult}
 */
export function useTaskDashboardStats() {
  return useQuery({
    queryKey: taskKeys.dashboardStats(),
    queryFn: getTaskDashboardStats,
    staleTime: 20_000,
    refetchInterval: 30_000,
    refetchIntervalInBackground: false,
  });
}

/**
 * Fetches workload summary for all employees.
 *
 * @returns {import('@tanstack/react-query').UseQueryResult}
 */
export function useTaskWorkloadSummary() {
  return useQuery({
    queryKey: taskKeys.workloadSummary(),
    queryFn: getTaskWorkloadSummary,
    staleTime: 25_000,
    refetchInterval: 30_000,
    refetchIntervalInBackground: false,
  });
}

/**
 * Fetches workload info for a single employee.
 *
 * @param {string} employeeId
 * @returns {import('@tanstack/react-query').UseQueryResult}
 */
export function useTaskWorkload(employeeId) {
  return useQuery({
    queryKey: taskKeys.workload(employeeId),
    queryFn: () => getTaskWorkload(employeeId),
    enabled: Boolean(employeeId),
    staleTime: 25_000,
  });
}

// ── Mutation hooks ───────────────────────────────────────────────────────────

/**
 * Creates a new task.
 *
 * @returns {import('@tanstack/react-query').UseMutationResult}
 */
export function useCreateTask() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload) => createTask(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: taskKeys.all });
    },
  });
}

/**
 * Updates an existing task.
 *
 * @returns {import('@tanstack/react-query').UseMutationResult}
 */
export function useUpdateTask() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }) => updateTask(id, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: taskKeys.all });
    },
  });
}

/**
 * Updates only the status of a task.
 *
 * @returns {import('@tanstack/react-query').UseMutationResult}
 */
export function useUpdateTaskStatus() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, status }) => updateTaskStatus(id, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: taskKeys.all });
    },
  });
}

/**
 * Deletes a task by ID.
 *
 * @returns {import('@tanstack/react-query').UseMutationResult}
 */
export function useDeleteTask() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id) => deleteTask(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: taskKeys.all });
    },
  });
}

/**
 * Creates a comment on a task.
 *
 * @returns {import('@tanstack/react-query').UseMutationResult}
 */
export function useCreateTaskComment() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ taskId, content }) => createTaskComment(taskId, content),
    onSuccess: (_, { taskId }) => {
      queryClient.invalidateQueries({ queryKey: taskKeys.comments(taskId) });
    },
  });
}

/**
 * Uploads a reference attachment to a task.
 *
 * @returns {import('@tanstack/react-query').UseMutationResult}
 */
export function useUploadTaskAttachment() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ taskId, file }) => uploadTaskAttachment(taskId, file),
    onSuccess: (_, { taskId }) => {
      queryClient.invalidateQueries({ queryKey: taskKeys.attachments(taskId) });
    },
  });
}

/**
 * Deletes a reference attachment from a task.
 *
 * @returns {import('@tanstack/react-query').UseMutationResult}
 */
export function useDeleteTaskAttachment() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ taskId, attachmentId }) => deleteTaskAttachment(taskId, attachmentId),
    onSuccess: (_, { taskId }) => {
      queryClient.invalidateQueries({ queryKey: taskKeys.attachments(taskId) });
    },
  });
}

/**
 * Reassigns a task to a different employee.
 * Invalidates all task queries after reassignment.
 *
 * @returns {import('@tanstack/react-query').UseMutationResult}
 */
export function useReassignTask() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ taskId, newEmployeeId, reason }) => reassignTask(taskId, newEmployeeId, reason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: taskKeys.all });
    },
  });
}
