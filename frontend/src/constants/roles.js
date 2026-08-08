/**
 * @fileoverview Application-wide constants for role names.
 *
 * Mirror the Spring Security role strings used in the backend so that
 * the frontend role-guard checks are always in sync with the API.
 */

/**
 * Enumeration of all application roles.
 * Values match the {@code ROLE_*} strings returned in the JWT payload.
 *
 * @readonly
 * @enum {string}
 */
export const ROLES = /** @type {const} */ ({
  ADMIN:    'ROLE_ADMIN',
  HR:       'ROLE_HR',
  MANAGER:  'ROLE_MANAGER',
  EMPLOYEE: 'ROLE_EMPLOYEE',
});
