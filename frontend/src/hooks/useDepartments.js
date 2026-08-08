/**
 * @fileoverview React Query hook for the department list used within the
 * employee module (form Autocomplete / filter dropdown).
 *
 * Wraps {@link getDepartments} with a long staleTime since department data
 * changes infrequently.
 */

import { useQuery } from '@tanstack/react-query';
import { getDepartments } from '@/services/departmentApi';
import { DEPARTMENT_QUERY_KEYS } from '@/constants/employeeConstants';

/**
 * Fetches all departments for use in the employee form Autocomplete and filter dropdowns.
 *
 * @returns {{
 *   data: import('@/services/departmentApi').DepartmentResponse[] | undefined,
 *   isLoading: boolean,
 *   isError: boolean,
 *   error: any
 * }}
 */
export function useDepartments() {
  const query = useQuery({
    queryKey: DEPARTMENT_QUERY_KEYS.list(),
    queryFn:  getDepartments,
    staleTime: 5 * 60_000, // departments change rarely
  });

  return {
    data:      query.data,
    isLoading: query.isLoading,
    isError:   query.isError,
    error:     query.error,
  };
}
