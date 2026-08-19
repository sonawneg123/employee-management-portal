/**
 * @fileoverview Task AI Review API service (Phase 7B).
 *
 * Wraps the Phase 7A backend endpoints:
 *  - POST /task-submissions/{submissionId}/ai-review    — request new AI review
 *  - GET  /task-submissions/{submissionId}/ai-review    — get latest AI review
 *  - GET  /task-submissions/{submissionId}/ai-reviews   — get all AI reviews
 *  - GET  /task-ai-reviews/{reviewId}                   — get review by its own ID
 *
 * Authorization: ADMIN, HR, MANAGER only (enforced by backend).
 * EMPLOYEE role is denied at the API layer.
 */

import axiosInstance from '@/api/axiosInstance';
import { API_ENDPOINTS } from '@/constants/api';

/**
 * @typedef {Object} TaskAiReviewResponse
 * @property {string}                id
 * @property {string}                taskId
 * @property {string}                submissionId
 * @property {string}                requestedById
 * @property {string}                requestedByName
 * @property {'PENDING'|'PROCESSING'|'COMPLETED'|'FAILED'} status
 * @property {string}                aiProvider
 * @property {string|null}           aiModel
 * @property {string}                promptVersion
 * @property {number|null}           completionScore
 * @property {number|null}           qualityScore
 * @property {number|null}           confidence
 * @property {'APPROVE'|'REQUEST_CHANGES'|'MANUAL_REVIEW'|null} recommendedAction
 * @property {string|null}           structuredAnalysisJson
 * @property {string|null}           managerSummary
 * @property {string|null}           errorMessage
 * @property {string}                createdAt
 * @property {string|null}           completedAt
 */

/**
 * Requests a new AI analysis of a task submission.
 * Returns 409 if a PENDING/PROCESSING review already exists.
 *
 * @param {string} submissionId UUID of the task submission
 * @returns {Promise<TaskAiReviewResponse>}
 */
export async function requestAiReview(submissionId) {
  const { data } = await axiosInstance.post(API_ENDPOINTS.TASK_SUBMISSION_AI_REVIEW(submissionId));
  return data;
}

/**
 * Returns the most recent AI review for the given submission.
 * Throws 404 if no review exists.
 *
 * @param {string} submissionId UUID of the task submission
 * @returns {Promise<TaskAiReviewResponse>}
 */
export async function getLatestAiReview(submissionId) {
  const { data } = await axiosInstance.get(API_ENDPOINTS.TASK_SUBMISSION_AI_REVIEW(submissionId));
  return data;
}

/**
 * Returns all AI reviews for the given submission, newest first.
 *
 * @param {string} submissionId UUID of the task submission
 * @returns {Promise<TaskAiReviewResponse[]>}
 */
export async function getAllAiReviews(submissionId) {
  const { data } = await axiosInstance.get(API_ENDPOINTS.TASK_SUBMISSION_AI_REVIEWS(submissionId));
  return data;
}

/**
 * Returns a specific AI review by its own UUID.
 *
 * @param {string} reviewId UUID of the AI review record
 * @returns {Promise<TaskAiReviewResponse>}
 */
export async function getAiReviewById(reviewId) {
  const { data } = await axiosInstance.get(API_ENDPOINTS.TASK_AI_REVIEW_BY_ID(reviewId));
  return data;
}

// ── Phase 7D: Employee AI feedback & manager analytics ─────────────────────────

/**
 * @typedef {Object} AiFeedbackResponse
 * @property {string} id
 * @property {string} submissionId
 * @property {'PENDING'|'PROCESSING'|'COMPLETED'|'FAILED'} status
 * @property {number|null} overallScore
 * @property {number|null} workQualityScore
 * @property {number|null} completenessScore
 * @property {number|null} relevanceScore
 * @property {string|null} summary
 * @property {string[]} strengths
 * @property {string[]} areasToImprove
 * @property {string[]} suggestionsForNextSubmission
 * @property {string|null} evaluatedAt
 * @property {string|null} requestedAt
 * @property {string} evaluationExplanation
 */

/**
 * @typedef {Object} AiScoreTrendResponse
 * @property {string} taskId
 * @property {Array<{reviewId: string, submissionNumber: number, overallScore: number, qualityScore: number|null, evaluatedAt: string}>} scoreHistory
 * @property {'IMPROVING'|'STABLE'|'DECLINING'|'INSUFFICIENT_DATA'} trendDirection
 * @property {number|null} totalScoreChange
 * @property {number|null} latestScore
 * @property {number|null} previousScore
 * @property {number|null} latestScoreChange
 * @property {boolean} hasTrendData
 */

/**
 * @typedef {Object} AiDashboardSummaryResponse
 * @property {number} totalEvaluated
 * @property {number|null} averageScore
 * @property {number} employeesImproving
 * @property {number} employeesNeedingAttention
 * @property {number} submissionsAwaitingEvaluation
 * @property {number} failedEvaluations
 */

/**
 * Returns the employee-safe AI feedback for a submission.
 * Employees may only view their own submission's feedback.
 * Does NOT expose managerSummary, recommendedAction, or AI internals.
 *
 * @param {string} submissionId UUID of the submission
 * @returns {Promise<AiFeedbackResponse>}
 */
export async function getEmployeeAiFeedback(submissionId) {
  const { data } = await axiosInstance.get(API_ENDPOINTS.TASK_SUBMISSION_AI_FEEDBACK(submissionId));
  return data;
}

/**
 * Returns the AI evaluation history for a submission (employee-safe view).
 *
 * @param {string} submissionId UUID of the submission
 * @returns {Promise<AiFeedbackResponse[]>}
 */
export async function getEmployeeAiHistory(submissionId) {
  const { data } = await axiosInstance.get(API_ENDPOINTS.TASK_SUBMISSION_AI_HISTORY(submissionId));
  return data;
}

/**
 * Returns the AI score trend for a task (manager view).
 * Requires ADMIN, HR, or MANAGER role.
 *
 * @param {string} taskId UUID of the task
 * @returns {Promise<AiScoreTrendResponse>}
 */
export async function getAiScoreTrend(taskId) {
  const { data } = await axiosInstance.get(API_ENDPOINTS.TASK_AI_TREND(taskId));
  return data;
}

/**
 * Returns AI task insights for the given task (manager view).
 * No new AI API calls — aggregates stored evaluation data.
 *
 * @param {string} taskId UUID of the task
 * @returns {Promise<import('../constants/api').AiTaskInsightsResponse>}
 */
export async function getAiTaskInsights(taskId) {
  const { data } = await axiosInstance.get(API_ENDPOINTS.TASK_AI_INSIGHTS(taskId));
  return data;
}

/**
 * Returns the AI summary for the manager dashboard.
 * Requires ADMIN, HR, or MANAGER role.
 *
 * @returns {Promise<AiDashboardSummaryResponse>}
 */
export async function getAiDashboardSummary() {
  const { data } = await axiosInstance.get(API_ENDPOINTS.AI_DASHBOARD_SUMMARY);
  return data;
}
