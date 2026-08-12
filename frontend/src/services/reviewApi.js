/**
 * reviewApi.js
 * API client for Performance Review endpoints.
 */
import axiosInstance from '@/api/axiosInstance';

const BASE = '/reviews';

/**
 * GET /reviews
 * @param {{ employeeId?: string, page?: number, size?: number, sortBy?: string, sortDir?: string }} params
 * @returns {Promise<PageResponse<ReviewResponse>>}
 */
export const getReviews = (params = {}) =>
  axiosInstance.get(BASE, { params }).then((r) => r.data);

/**
 * GET /reviews/:id
 * @param {string} id
 * @returns {Promise<ReviewResponse>}
 */
export const getReviewById = (id) =>
  axiosInstance.get(`${BASE}/${id}`).then((r) => r.data);

/**
 * POST /reviews
 * @param {{ employeeId: string, reviewPeriod: string, rating: number, reviewDate: string, comments?: string, goals?: string }} data
 * @returns {Promise<ReviewResponse>}
 */
export const createReview = (data) =>
  axiosInstance.post(BASE, data).then((r) => r.data);

/**
 * PUT /reviews/:id
 * @param {string} id
 * @param {{ reviewPeriod: string, rating: number, reviewDate: string, comments?: string, goals?: string }} data
 * @returns {Promise<ReviewResponse>}
 */
export const updateReview = (id, data) =>
  axiosInstance.put(`${BASE}/${id}`, data).then((r) => r.data);

/**
 * DELETE /reviews/:id
 * @param {string} id
 * @returns {Promise<void>}
 */
export const deleteReview = (id) =>
  axiosInstance.delete(`${BASE}/${id}`);
