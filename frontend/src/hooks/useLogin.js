/**
 * @fileoverview useLogin hook — wraps the login mutation with React Query.
 *
 * Encapsulates the TanStack Query {@link useMutation} so that:
 * - The calling component is not coupled to the Query API.
 * - Success handling (session persistence + navigation) lives in AuthContext.
 * - Error state is normalised and ready to display in the form.
 *
 * The hook does NOT call {@link AuthContext.login} directly — it calls
 * {@link authApi.login} and delegates session persistence to {@link useAuth}.
 */

import { useMutation } from '@tanstack/react-query';
import { useAuth } from '@/hooks/useAuth';

/**
 * @typedef {Object} UseLoginReturn
 * @property {(payload: import('@/services/authApi').LoginPayload) => Promise<void>} mutate
 *   - Triggers the login mutation.
 * @property {import('@/services/authApi').LoginPayload | undefined} variables
 *   - The payload last passed to mutate.
 * @property {boolean}  isPending   - True while the request is in flight.
 * @property {boolean}  isError     - True when the last attempt failed.
 * @property {boolean}  isSuccess   - True after a successful login.
 * @property {import('@/api/axiosInstance').NormalisedError | null} error
 *   - The normalised error from the last failed attempt.
 * @property {() => void} reset     - Resets mutation state (clears error).
 */

/**
 * React Query mutation hook for user login.
 *
 * Calls {@link AuthContext.login} which handles token persistence and
 * navigation internally. Any thrown error propagates to {@code error}.
 *
 * @returns {UseLoginReturn}
 */
export function useLogin() {
  const { login } = useAuth();

  const mutation = useMutation({
    mutationFn: (/** @type {import('@/services/authApi').LoginPayload} */ payload) =>
      login(payload),
  });

  return {
    mutate: mutation.mutateAsync,
    variables: mutation.variables,
    isPending: mutation.isPending,
    isError: mutation.isError,
    isSuccess: mutation.isSuccess,
    error: /** @type {any} */ (mutation.error),
    reset: mutation.reset,
  };
}
