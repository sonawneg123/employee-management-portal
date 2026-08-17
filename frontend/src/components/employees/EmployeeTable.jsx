/**
 * @fileoverview EmployeeTable — sortable data table for the employee list.
 *
 * Renders a full MUI Table with:
 * - Sortable column headers (clicking toggles sort direction)
 * - Row-level actions menu via {@link EmployeeActionsMenu}
 * - Loading state via {@link EmployeeSkeleton}
 * - Empty state via {@link EmployeeEmptyState}
 * - Error state via {@link EmployeeErrorState}
 * - Responsive: hides on xs/sm in favour of card view (controlled by parent)
 */

import React, { useState } from 'react';
import {
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TableSortLabel,
} from '@mui/material';
import MoreVertIcon from '@mui/icons-material/MoreVert';
import { IconButton, Tooltip } from '@mui/material';
import { getEmployeeColumns } from '@/utils/employeeColumns';
import EmployeeActionsMenu from './EmployeeActionsMenu';
import EmployeeSkeleton from './EmployeeSkeleton';
import EmployeeEmptyState from './EmployeeEmptyState';
import EmployeeErrorState from './EmployeeErrorState';

/**
 * @typedef {Object} EmployeeTableProps
 * @property {import('@/services/employeeApi').EmployeeResponse[]} employees
 * @property {boolean}   isLoading
 * @property {boolean}   isFetching
 * @property {boolean}   isError
 * @property {any}       error
 * @property {string}    sort           - Active sort field.
 * @property {'asc'|'desc'} direction  - Active sort direction.
 * @property {boolean}   hasFilters     - Whether any filter/search is active.
 * @property {boolean}   canEdit        - Role guard: show edit action.
 * @property {boolean}   canDelete      - Role guard: show delete action.
 * @property {(field: string, dir: 'asc'|'desc') => void} onSort
 * @property {(employee: Object) => void} onView
 * @property {(employee: Object) => void} onEdit
 * @property {(employee: Object) => void} onDelete
 * @property {() => void} onRetry
 * @property {() => void} onClearFilters
 * @property {() => void} [onAdd]
 * @property {boolean}   [canCreate]
 */

/**
 * Full data table for the employee list page (desktop layout).
 *
 * @param {EmployeeTableProps} props
 * @returns {JSX.Element}
 */
export default function EmployeeTable({
  employees,
  isLoading,
  isFetching,
  isError,
  error,
  sort,
  direction,
  hasFilters,
  canEdit,
  canDelete,
  onSort,
  onView,
  onEdit,
  onDelete,
  onRetry,
  onClearFilters,
  onAdd,
  canCreate,
}) {
  const columns = getEmployeeColumns();

  // ── Actions menu state ────────────────────────────────────────────────────
  const [menuAnchor, setMenuAnchor] = useState(null);
  const [menuEmployee, setMenuEmployee] = useState(null);

  /**
   * @param {React.MouseEvent} e
   * @param {Object} employee
   */
  const handleMenuOpen = (e, employee) => {
    e.stopPropagation();
    setMenuAnchor(e.currentTarget);
    setMenuEmployee(employee);
  };

  const handleMenuClose = () => {
    setMenuAnchor(null);
    setMenuEmployee(null);
  };

  // ── Sort handler ──────────────────────────────────────────────────────────

  /**
   * Toggles direction if the same field is clicked, otherwise defaults to 'asc'.
   *
   * @param {string} field
   */
  const handleSort = (field) => {
    if (field === sort) {
      onSort(field, direction === 'asc' ? 'desc' : 'asc');
    } else {
      onSort(field, 'asc');
    }
  };

  // ── Render states ─────────────────────────────────────────────────────────

  if (isError) {
    return <EmployeeErrorState error={error} onRetry={onRetry} />;
  }

  return (
    <>
      <TableContainer component={Paper} variant="outlined" sx={{ borderRadius: 0, border: 'none' }}>
        <Table stickyHeader aria-label="Employee list table" aria-busy={isLoading || isFetching}>
          <TableHead>
            <TableRow>
              {columns.map((col) => (
                <TableCell
                  key={col.id}
                  align={col.align ?? 'left'}
                  sx={{
                    minWidth: col.width,
                    fontWeight: 700,
                    bgcolor: 'background.default',
                    fontSize: '0.72rem',
                    letterSpacing: '0.06em',
                    textTransform: 'uppercase',
                    color: 'text.secondary',
                  }}
                >
                  {col.sortable ? (
                    <TableSortLabel
                      active={sort === col.id}
                      direction={sort === col.id ? direction : 'asc'}
                      onClick={() => handleSort(col.id)}
                      aria-label={`Sort by ${col.label}`}
                    >
                      {col.label}
                    </TableSortLabel>
                  ) : (
                    col.label
                  )}
                </TableCell>
              ))}
              {/* Actions column header */}
              <TableCell sx={{ width: 48, bgcolor: 'background.default' }} />
            </TableRow>
          </TableHead>

          <TableBody>
            {isLoading ? (
              <TableRow>
                <TableCell colSpan={columns.length + 1} sx={{ p: 0, border: 'none' }}>
                  <EmployeeSkeleton rows={8} columns={columns.length + 1} />
                </TableCell>
              </TableRow>
            ) : employees.length === 0 ? (
              <TableRow>
                <TableCell colSpan={columns.length + 1} sx={{ border: 'none' }}>
                  <EmployeeEmptyState
                    hasFilters={hasFilters}
                    onClear={onClearFilters}
                    onAdd={onAdd}
                    canCreate={canCreate}
                  />
                </TableCell>
              </TableRow>
            ) : (
              employees.map((emp) => (
                <TableRow
                  key={emp.id}
                  hover
                  onClick={() => onView(emp)}
                  sx={{
                    cursor: 'pointer',
                    opacity: emp.status === 'DISABLED' ? 0.55 : 1,
                    '&:hover': { bgcolor: (t) => `${t.palette.primary.main}08` },
                    transition: 'background-color 150ms ease, opacity 150ms ease',
                    ...(emp.status === 'DISABLED' && {
                      bgcolor: 'action.hover',
                    }),
                  }}
                  aria-label={`Row for ${emp.firstName} ${emp.lastName}${emp.status === 'DISABLED' ? ' (disabled)' : ''}`}
                >
                  {columns.map((col) => (
                    <TableCell key={col.id} align={col.align ?? 'left'}>
                      {col.render(emp)}
                    </TableCell>
                  ))}
                  <TableCell align="right" onClick={(e) => e.stopPropagation()}>
                    <Tooltip title="Actions">
                      <IconButton
                        size="small"
                        onClick={(e) => handleMenuOpen(e, emp)}
                        aria-label={`Actions for ${emp.firstName} ${emp.lastName}`}
                      >
                        <MoreVertIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>

      {/* Per-row actions menu */}
      <EmployeeActionsMenu
        anchorEl={menuAnchor}
        open={Boolean(menuAnchor)}
        onClose={handleMenuClose}
        onView={() => menuEmployee && onView(menuEmployee)}
        onEdit={() => menuEmployee && onEdit(menuEmployee)}
        onDelete={() => menuEmployee && onDelete(menuEmployee)}
        canEdit={canEdit}
        canDelete={canDelete}
      />
    </>
  );
}
