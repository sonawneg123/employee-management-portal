/**
 * @fileoverview useAuth hook — re-exports the AuthContext consumer.
 *
 * Provides a single, consistent import point for all auth state and actions.
 * Consumers should always import {@code useAuth} from this module rather than
 * reaching into the context directly.
 */

export { useAuth } from '@/contexts/AuthContext';
