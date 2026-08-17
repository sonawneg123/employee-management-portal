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
  const { data } = await axiosInstance.post(
    API_ENDPOINTS.TASK_SUBMISSION_AI_REVIEW(submissionId),
  );
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
  const { data } = await axiosInstance.get(
    API_ENDPOINTS.TASK_SUBMISSION_AI_REVIEW(submissionId),
  );
  return data;
}

/**
 * Returns all AI reviews for the given submission, newest first.
 *
 * @param {string} submissionId UUID of the task submission
 * @returns {Promise<TaskAiReviewResponse[]>}
 */
export async function getAllAiReviews(submissionId) {
  const { data } = await axiosInstance.get(
    API_ENDPOINTS.TASK_SUBMISSION_AI_REVIEWS(submissionId),
  );
  return data;
}

/**
 * Returns a specific AI review by its own UUID.
 *
 * @param {string} reviewId UUID of the AI review record
 * @returns {Promise<TaskAiReviewResponse>}
 */
export async function getAiReviewById(reviewId) {
  const { data } = await axiosInstance.get(
    API_ENDPOINTS.TASK_AI_REVIEW_BY_ID(reviewId),
  );
  return data;
}
