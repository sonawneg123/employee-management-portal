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

/**
 * Requests a password-reset OTP for the given email address.
 *
 * @param {{ email: string }} payload
 * @returns {Promise<{ message: string }>}
 */
export async function forgotPassword(payload) {
  const { data } = await axiosInstance.post(API_ENDPOINTS.AUTH_FORGOT_PASSWORD, payload);
  return data;
}

/**
 * Verifies the OTP submitted by the user.
 *
 * @param {{ email: string, otp: string }} payload
 * @returns {Promise<{ message: string }>}
 */
export async function verifyOtp(payload) {
  const { data } = await axiosInstance.post(API_ENDPOINTS.AUTH_VERIFY_OTP, payload);
  return data;
}

/**
 * Resets the user's password after OTP verification.
 *
 * @param {{ email: string, newPassword: string, confirmPassword: string }} payload
 * @returns {Promise<{ message: string }>}
 */
export async function resetPassword(payload) {
  const { data } = await axiosInstance.post(API_ENDPOINTS.AUTH_RESET_PASSWORD, payload);
  return data;
}
