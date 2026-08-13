/**
 * @fileoverview useRegister hook — wraps the register mutation with React Query.
 *
 * Encapsulates the TanStack Query {@link useMutation} for account creation.
 * Delegates session persistence and navigation to {@link AuthContext.register}.
 */

import { useMutation } from '@tanstack/react-query';
import { useAuth } from '@/hooks/useAuth';

/**
 * @typedef {Object} UseRegisterReturn
 * @property {(payload: import('@/services/authApi').RegisterPayload) => Promise<void>} mutate
 * @property {boolean}  isPending
 * @property {boolean}  isError
 * @property {boolean}  isSuccess
 * @property {import('@/api/axiosInstance').NormalisedError | null} error
 * @property {() => void} reset
 */

/**
 * React Query mutation hook for user registration.
 *
 * Calls {@link AuthContext.register} which handles token persistence and
 * navigation internally. Any thrown error propagates to {@code error}.
 *
 * @returns {UseRegisterReturn}
 */
export function useRegister() {
  const { register } = useAuth();

  const mutation = useMutation({
    mutationFn: (/** @type {import('@/services/authApi').RegisterPayload} */ payload) =>
      register(payload),
  });

  return {
    mutate: mutation.mutateAsync,
    isPending: mutation.isPending,
    isError: mutation.isError,
    isSuccess: mutation.isSuccess,
    error: /** @type {any} */ (mutation.error),
    reset: mutation.reset,
  };
}
