/**
 * @fileoverview Axios instance and interceptor configuration.
 *
 * All HTTP requests in the application must go through this instance so that:
 * - The base URL and timeout are applied uniformly.
 * - The JWT Bearer token is injected on every outbound request.
 * - 401 responses trigger an automatic logout and redirect to /login.
 * - Error responses are normalised into a consistent shape before reaching
 *   the calling code.
 */

import axios from 'axios';
import { API_BASE_URL, REQUEST_TIMEOUT_MS, TOKEN_STORAGE_KEY } from '@/constants/api';
import { getItem, clearAll } from '@/utils/localStorage';
import { ROUTES } from '@/constants/routes';

/**
 * The shared Axios instance used by every API service module.
 *
 * @type {import('axios').AxiosInstance}
 */
const axiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: REQUEST_TIMEOUT_MS,
  headers: {
    'Content-Type': 'application/json',
    Accept:         'application/json',
  },
});

// ── Request interceptor — attach Bearer token ────────────────────────────────

axiosInstance.interceptors.request.use(
  (config) => {
    const token = getItem(TOKEN_STORAGE_KEY);
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error),
);

// ── Response interceptor — handle 401 and normalise errors ──────────────────

axiosInstance.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;

    // Automatic logout on 401 — token expired or invalid
    if (status === 401) {
      clearAll();
      // Redirect to login while preserving the attempted URL for post-login redirect
      const current = window.location.pathname;
      const redirect = current !== ROUTES.LOGIN ? `?redirect=${encodeURIComponent(current)}` : '';
      window.location.href = `${ROUTES.LOGIN}${redirect}`;
      return Promise.reject(normaliseError(error));
    }

    return Promise.reject(normaliseError(error));
  },
);

/**
 * @typedef {Object} NormalisedError
 * @property {number}            status      - HTTP status code.
 * @property {string}            title       - Short error title.
 * @property {string}            message     - Human-readable detail message.
 * @property {Record<string,string>} [violations] - Field-level validation errors.
 * @property {boolean}           isNetwork   - True when no response was received.
 */

/**
 * Normalises an Axios error into a consistent shape so that UI components
 * can render errors without parsing raw Axios objects.
 *
 * Supports the RFC 7807 {@code ProblemDetail} response format returned by the
 * Spring Boot backend.
 *
 * @param {import('axios').AxiosError} error - The raw Axios error.
 * @returns {NormalisedError} A normalised error object.
 */
export function normaliseError(error) {
  if (!error.response) {
    return {
      status:    0,
      title:     'Network Error',
      message:   'Unable to reach the server. Please check your connection.',
      isNetwork: true,
    };
  }

  const { data, status } = error.response;

  // RFC 7807 ProblemDetail shape from the Spring Boot backend
  if (data && data.title) {
    return {
      status,
      title:      data.title,
      message:    data.detail ?? data.title,
      violations: data.properties?.violations ?? null,
      isNetwork:  false,
    };
  }

  // Fallback for non-ProblemDetail error bodies
  const defaultMessages = {
    400: 'The request contains invalid data.',
    401: 'Your session has expired. Please log in again.',
    403: 'You do not have permission to perform this action.',
    404: 'The requested resource was not found.',
    409: 'This resource already exists.',
    500: 'An unexpected server error occurred. Please try again later.',
  };

  return {
    status,
    title:     'Error',
    message:   defaultMessages[status] ?? 'An unexpected error occurred.',
    isNetwork: false,
  };
}

export default axiosInstance;
