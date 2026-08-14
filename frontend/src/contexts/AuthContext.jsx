/**
 * @fileoverview AuthContext — application-wide authentication state management.
 *
 * Provides login, logout, register, and permission-checking helpers to any
 * component in the tree. The JWT access token and the minimal user object
 * are persisted to localStorage so that the session survives a page refresh.
 *
 * Automatic logout is triggered when the stored token is found to be expired
 * on the next render cycle.
 */

import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import * as authApi from '@/services/authApi';
import { getItem, setItem, removeItem } from '@/utils/localStorage';
import { isTokenExpired } from '@/utils/jwtUtils';
import { TOKEN_STORAGE_KEY, USER_STORAGE_KEY } from '@/constants/api';
import { ROUTES } from '@/constants/routes';
import { ROLES } from '@/constants/roles';

/**
 * Resolves the home dashboard path for a given roles array.
 *
 * @param {string[]} roles
 * @returns {string}
 */
function resolveDashboard(roles = []) {
  if (roles.includes(ROLES.ADMIN)) return ROUTES.ADMIN_DASHBOARD;
  if (roles.includes(ROLES.HR) || roles.includes(ROLES.MANAGER)) return ROUTES.HR_DASHBOARD;
  return ROUTES.EMPLOYEE_DASHBOARD;
}

/**
 * @typedef {Object} AuthUser
 * @property {string}   userId
 * @property {string}   email
 * @property {string}   firstName
 * @property {string}   lastName
 * @property {string[]} roles
 */

/**
 * @typedef {Object} AuthContextValue
 * @property {AuthUser|null}  user              - The currently authenticated user, or null.
 * @property {string|null}    token             - The current JWT access token, or null.
 * @property {boolean}        isAuthenticated   - Whether the user is currently authenticated.
 * @property {boolean}        isLoading         - Whether an auth operation is in progress.
 * @property {(payload: import('@/services/authApi').LoginPayload) => Promise<void>} login
 * @property {(payload: import('@/services/authApi').RegisterPayload) => Promise<void>} register
 * @property {() => void}    logout            - Clears session and redirects to /login.
 * @property {(patch: Partial<AuthUser>) => void} updateUser - Merges a patch into the stored user without re-login.
 * @property {(role: string) => boolean}        hasRole       - Returns true if the user has the given role.
 * @property {(roles: string[]) => boolean}     hasAnyRole    - Returns true if the user has at least one of the given roles.
 */

/** @type {React.Context<AuthContextValue>} */
const AuthContext = createContext(/** @type {AuthContextValue} */ ({}));

/**
 * Authentication context provider.
 *
 * Must be placed inside {@code <BrowserRouter>} so that {@link useNavigate}
 * is available.
 *
 * @param {{ children: React.ReactNode }} props
 * @returns {JSX.Element}
 */
export function AuthProvider({ children }) {
  const navigate = useNavigate();

  const [token, setToken] = useState(/** @type {string|null} */ (getItem(TOKEN_STORAGE_KEY)));
  const [user, setUser] = useState(/** @type {AuthUser|null} */ (getItem(USER_STORAGE_KEY)));
  const [isLoading, setIsLoading] = useState(false);

  // ── Auto-logout when stored token is expired ──────────────────────────────
  useEffect(() => {
    if (token && isTokenExpired(token)) {
      logout();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token]);

  /**
   * Persists session data to state and localStorage.
   *
   * @param {import('@/services/authApi').AuthResponse} authResponse
   */
  const persistSession = useCallback((authResponse) => {
    const { accessToken, userId, email, firstName, lastName, roles } = authResponse;

    /** @type {AuthUser} */
    const userObj = { userId, email, firstName, lastName, roles };

    setItem(TOKEN_STORAGE_KEY, accessToken);
    setItem(USER_STORAGE_KEY, userObj);
    setToken(accessToken);
    setUser(userObj);
  }, []);

  /**
   * Logs in the user with email and password, persists the session, and
   * navigates to the dashboard.
   *
   * @param {import('@/services/authApi').LoginPayload} payload
   * @returns {Promise<void>}
   * @throws Will re-throw any API error so the form can display it.
   */
  const login = useCallback(
    async (payload) => {
      setIsLoading(true);
      try {
        const response = await authApi.login(payload);
        persistSession(response);

        // Honour the redirect query param set by the Axios 401 interceptor;
        // fall back to the role-appropriate dashboard instead of /dashboard.
        const params = new URLSearchParams(window.location.search);
        const redirect = params.get('redirect') ?? resolveDashboard(response.roles ?? []);
        navigate(redirect, { replace: true });
      } finally {
        setIsLoading(false);
      }
    },
    [navigate, persistSession],
  );

  /**
   * Registers a new account, persists the session, and navigates to the dashboard.
   *
   * @param {import('@/services/authApi').RegisterPayload} payload
   * @returns {Promise<void>}
   * @throws Will re-throw any API error so the form can display it.
   */
  const register = useCallback(
    async (payload) => {
      setIsLoading(true);
      try {
        const response = await authApi.register(payload);
        persistSession(response);
        navigate(resolveDashboard(response.roles ?? []), { replace: true });
      } finally {
        setIsLoading(false);
      }
    },
    [navigate, persistSession],
  );

  /**
   * Clears the session and redirects to the login page.
   *
   * @returns {void}
   */
  const logout = useCallback(() => {
    removeItem(TOKEN_STORAGE_KEY);
    removeItem(USER_STORAGE_KEY);
    setToken(null);
    setUser(null);
    navigate('/login', { replace: true });
  }, [navigate]);

  /**
   * Merges a partial patch into the persisted user object (state + localStorage).
   *
   * Used by profile-edit flows to reflect name changes in the topbar/sidebar
   * without requiring a full re-login.
   *
   * @param {Partial<AuthUser>} patch - Fields to merge (e.g. { firstName, lastName }).
   * @returns {void}
   */
  const updateUser = useCallback((patch) => {
    setUser((prev) => {
      if (!prev) return prev;
      const next = { ...prev, ...patch };
      setItem(USER_STORAGE_KEY, next);
      return next;
    });
  }, []);

  /**
   * Returns whether the authenticated user possesses the given role.
   *
   * @param {string} role - Role string to check (e.g., {@code "ROLE_ADMIN"}).
   * @returns {boolean}
   */
  const hasRole = useCallback((role) => user?.roles?.includes(role) ?? false, [user]);

  /**
   * Returns whether the authenticated user possesses at least one of the
   * supplied roles.
   *
   * @param {string[]} roles - Role strings to check.
   * @returns {boolean}
   */
  const hasAnyRole = useCallback((roles) => roles.some((r) => user?.roles?.includes(r)), [user]);

  const isAuthenticated = Boolean(token && user && !isTokenExpired(token));

  const value = useMemo(
    () => ({
      user,
      token,
      isAuthenticated,
      isLoading,
      login,
      register,
      logout,
      updateUser,
      hasRole,
      hasAnyRole,
    }),
    [
      user,
      token,
      isAuthenticated,
      isLoading,
      login,
      register,
      logout,
      updateUser,
      hasRole,
      hasAnyRole,
    ],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

/**
 * Hook to access the authentication context.
 *
 * @returns {AuthContextValue}
 */
// eslint-disable-next-line react-refresh/only-export-components
export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside <AuthProvider>');
  return ctx;
}

export { AuthContext };
