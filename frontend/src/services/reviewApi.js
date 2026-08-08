/**
 * @fileoverview Performance Review API service.
 */

import axiosInstance from '@/api/axiosInstance';
import { API_ENDPOINTS } from '@/constants/api';

/**
 * @typedef {Object} PerformanceReviewResponse
 * @property {string}      id
 * @property {string}      employeeId
 * @property {string|null} reviewerId
 * @property {string}      reviewPeriod
 * @property {number}      rating
 * @property {string|null} comments
 * @property {string|null} goals
 * @property {string}      reviewDate
 * @property {string}      createdAt
 * @property {string}      updatedAt
 */

/**
 * Returns a paginated list of performance reviews.
 *
 * @param {Object} [params={}]
 * @returns {Promise<import('./employeeApi').PageResponse<PerformanceReviewResponse>>}
 */
export async function getReviews(params = {}) {
  const { data } = await axiosInstance.get(API_ENDPOINTS.REVIEWS, { params });
  return data;
}

/**
 * Returns a single performance review by UUID.
 *
 * @param {string} id
 * @returns {Promise<PerformanceReviewResponse>}
 */
export async function getReviewById(id) {
  const { data } = await axiosInstance.get(API_ENDPOINTS.REVIEW_BY_ID(id));
  return data;
}

/**
 * Creates a new performance review (Manager/Admin only).
 *
 * @param {Object} payload
 * @returns {Promise<PerformanceReviewResponse>}
 */
export async function createReview(payload) {
  const { data } = await axiosInstance.post(API_ENDPOINTS.REVIEWS, payload);
  return data;
}

/**
 * Updates an existing performance review.
 *
 * @param {string} id
 * @param {Object} payload
 * @returns {Promise<PerformanceReviewResponse>}
 */
export async function updateReview(id, payload) {
  const { data } = await axiosInstance.put(API_ENDPOINTS.REVIEW_BY_ID(id), payload);
  return data;
}

/**
 * Deletes a performance review.
 *
 * @param {string} id
 * @returns {Promise<void>}
 */
export async function deleteReview(id) {
  await axiosInstance.delete(API_ENDPOINTS.REVIEW_BY_ID(id));
}
