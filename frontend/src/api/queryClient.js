/**
 * @fileoverview TanStack React Query client configuration.
 *
 * The {@link QueryClient} is instantiated once and shared through
 * {@link QueryClientProvider} in {@code main.jsx}. Default options are
 * configured conservatively for an enterprise portal:
 * - Stale time of 2 minutes prevents unnecessary re-fetches.
 * - Retry only once on failure (backend errors should not be spammed).
 * - Cache time of 5 minutes keeps data available for back-navigation.
 * - Window focus re-fetching is disabled (users work on the portal, tabs
 *   don't alt-tab constantly).
 */

import { QueryClient } from '@tanstack/react-query';

/**
 * Shared React Query client instance.
 *
 * @type {QueryClient}
 */
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime:         2 * 60 * 1000,  // 2 minutes
      gcTime:            5 * 60 * 1000,  // 5 minutes (formerly cacheTime)
      retry:             1,
      retryDelay:        (attempt) => Math.min(1000 * 2 ** attempt, 10_000),
      refetchOnWindowFocus: false,
      refetchOnReconnect:   true,
    },
    mutations: {
      retry: 0,  // Never retry mutations — side effects must be explicit
    },
  },
});
