/**
 * @fileoverview useAttendanceStatus — provides the authenticated employee's
 * attendance state for today.
 *
 * Used by the Sidebar to determine whether to grey-out the Tasks navigation
 * when an employee has checked out.
 */

import { useQuery } from '@tanstack/react-query';
import { getMyAttendance } from '@/services/attendanceApi';
import { useAuth } from '@/contexts/AuthContext';
import { ROLES } from '@/constants/roles';

/**
 * Returns today's attendance record for the authenticated employee,
 * or `null` if none exists (not yet checked in).
 *
 * Only enabled for EMPLOYEE role — privileged users don't have this restriction.
 *
 * @returns {{ todayAttendance: Object|null, isCheckedOut: boolean, isLoading: boolean }}
 */
export function useTodayAttendance() {
  const { user, hasAnyRole } = useAuth();

  const isEmployee =
    Boolean(user) &&
    hasAnyRole([ROLES.EMPLOYEE]) &&
    !hasAnyRole([ROLES.HR, ROLES.MANAGER, ROLES.ADMIN]);

  const today = new Date().toISOString().split('T')[0];

  const { data, isLoading } = useQuery({
    queryKey: ['attendance', 'my-today', today],
    queryFn: () => getMyAttendance({ date: today, page: 0, size: 1 }),
    enabled: isEmployee,
    staleTime: 30_000,
    refetchInterval: 30_000,
    refetchIntervalInBackground: false,
  });

  const todayAttendance = data?.content?.[0] ?? null;

  // The employee is considered "checked out" when:
  //  - they have a record for today AND
  //  - checkOutTime is non-null
  const isCheckedOut = Boolean(
    todayAttendance &&
    todayAttendance.checkOutTime !== null &&
    todayAttendance.checkOutTime !== undefined,
  );

  return { todayAttendance, isCheckedOut, isLoading };
}
