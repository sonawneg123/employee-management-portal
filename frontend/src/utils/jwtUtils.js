/**
 * @fileoverview JWT utility — decode and inspect token payload without verification.
 *
 * Verification is the server's responsibility. These helpers are used only for
 * reading claims (roles, expiry, subject) from the already-validated token
 * that the server issued.
 */

import { jwtDecode } from 'jwt-decode';

/**
 * @typedef {Object} JwtPayload
 * @property {string}   sub     - Subject (email address).
 * @property {number}   iat     - Issued-at timestamp (Unix seconds).
 * @property {number}   exp     - Expiration timestamp (Unix seconds).
 * @property {string[]} roles   - List of Spring Security role strings.
 */

/**
 * Decodes a JWT and returns its payload without verifying the signature.
 *
 * @param {string} token - The compact JWT string.
 * @returns {JwtPayload | null} The decoded payload, or {@code null} if the
 *   token is blank or cannot be decoded.
 */
export function decodeToken(token) {
  if (!token) return null;
  try {
    return /** @type {JwtPayload} */ (jwtDecode(token));
  } catch {
    return null;
  }
}

/**
 * Returns whether the given token has expired based on its {@code exp} claim.
 * A token is considered expired if the current time is at or past
 * {@code exp - 30 seconds} (30-second buffer to avoid edge-case races).
 *
 * @param {string} token - The compact JWT string.
 * @returns {boolean} {@code true} if the token is expired or invalid.
 */
export function isTokenExpired(token) {
  const payload = decodeToken(token);
  if (!payload || !payload.exp) return true;
  const nowSeconds = Math.floor(Date.now() / 1000);
  return payload.exp - 30 < nowSeconds;
}

/**
 * Extracts the subject (email address) from a JWT.
 *
 * @param {string} token - The compact JWT string.
 * @returns {string | null} The email address, or {@code null}.
 */
export function getTokenSubject(token) {
  return decodeToken(token)?.sub ?? null;
}

/**
 * Extracts the roles array from a JWT.
 *
 * @param {string} token - The compact JWT string.
 * @returns {string[]} Array of role strings (may be empty if none present).
 */
export function getTokenRoles(token) {
  return decodeToken(token)?.roles ?? [];
}

/**
 * Returns the expiration timestamp (Unix milliseconds) from a JWT.
 *
 * @param {string} token - The compact JWT string.
 * @returns {number | null} Expiry in Unix milliseconds, or {@code null}.
 */
export function getTokenExpiry(token) {
  const payload = decodeToken(token);
  if (!payload?.exp) return null;
  return payload.exp * 1000;
}
