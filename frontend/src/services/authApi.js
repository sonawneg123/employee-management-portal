/**
 * @fileoverview Authentication API service.
 *
 * All requests are made via the shared {@link axiosInstance} so that the
 * Bearer token interceptor and error normalisation apply automatically.
 */

import axiosInstance from '@/api/axiosInstance';
import { API_ENDPOINTS } from '@/constants/api';

/**
 * @typedef {Object} RegisterPayload
 * @property {string} email
 * @property {string} password
 * @property {string} firstName
 * @property {string} lastName
 */

/**
 * @typedef {Object} LoginPayload
 * @property {string} email
 * @property {string} password
 */

/**
 * @typedef {Object} AuthResponse
 * @property {string}   accessToken
 * @property {string}   tokenType
 * @property {number}   expiresIn
 * @property {string}   userId
 * @property {string}   email
 * @property {string}   firstName
 * @property {string}   lastName
 * @property {string[]} roles
 */

/**
 * Registers a new user account.
 *
 * @param {RegisterPayload} payload - Registration data.
 * @returns {Promise<AuthResponse>} The authentication response containing the JWT.
 */
export async function register(payload) {
  const { data } = await axiosInstance.post(API_ENDPOINTS.AUTH_REGISTER, payload);
  return data;
}

/**
 * Authenticates a user and returns a JWT token.
 *
 * @param {LoginPayload} payload - Login credentials.
 * @returns {Promise<AuthResponse>} The authentication response containing the JWT.
 */
export async function login(payload) {
  const { data } = await axiosInstance.post(API_ENDPOINTS.AUTH_LOGIN, payload);
  return data;
}
