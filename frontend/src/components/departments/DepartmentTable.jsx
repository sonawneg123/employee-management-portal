/**
 * @fileoverview DepartmentTable — sortable data table for the department list.
 *
 * Renders a full MUI Table with:
 * - Sortable column headers
 * - Per-row actions menu
 * - Loading/empty/error states
 */

import React, { useState } from 'react';
import {
  IconButton,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TableSortLabel,
  Tooltip,
} from '@mui/material';
import MoreVertIcon from '@mui/icons-material/MoreVert';
import { getDepartmentColumns } from '@/utils/departmentColumns';
import DepartmentActionsMenu from './DepartmentActionsMenu';
import DepartmentSkeleton from './DepartmentSkeleton';
import DepartmentEmptyState from './DepartmentEmptyState';
import DepartmentErrorState from './DepartmentErrorState';

/**
 * @typedef {Object} DepartmentTableProps
 * @property {import('@/services/departmentApi').DepartmentResponse[]} departments
 * @property {boolean}   isLoading
 * @property {boolean}   isFetching
 * @property {boolean}   isError
 * @property {any}       error
 * @property {string}    sort
 * @property {'asc'|'desc'} direction
 * @property {boolean}   hasSearch
 * @property {boolean}   canEdit
 * @property {boolean}   canDelete
 * @property {(field: string, dir: 'asc'|'desc') => void} onSort
 * @property {(dept: Object) => void} onView
 * @property {(dept: Object) => void} onEdit
 * @property {(dept: Object) => void} onDelete
 * @property {() => void} onRetry
 * @property {() => void} onClearSearch
 * @property {() => void} [onAdd]
 * @property {boolean}   [canCreate]
 */

/**
 * Department data table.
 *
 * @param {DepartmentTableProps} props
 * @returns {JSX.Element}
 */
export default function DepartmentTable({
  departments,
  isLoading,
  isFetching,
  isError,
  error,
  sort,
  direction,
  hasSearch,
  canEdit,
  canDelete,
  onSort,
  onView,
  onEdit,
  onDelete,
  onRetry,
  onClearSearch,
  onAdd,
  canCreate,
}) {
  const columns = getDepartmentColumns();

  const [menuAnchor, setMenuAnchor] = useState(null);
  const [menuDept, setMenuDept] = useState(null);

  const handleMenuOpen = (e, dept) => {
    e.stopPropagation();
    setMenuAnchor(e.currentTarget);
    setMenuDept(dept);
  };

  const handleMenuClose = () => {
    setMenuAnchor(null);
    setMenuDept(null);
  };

  const handleSort = (field) => {
    if (field === sort) {
      onSort(field, direction === 'asc' ? 'desc' : 'asc');
    } else {
      onSort(field, 'asc');
    }
  };

  if (isError) {
    return <DepartmentErrorState error={error} onRetry={onRetry} />;
  }

  return (
    <>
      <TableContainer component={Paper} variant="outlined" sx={{ borderRadius: 0, border: 'none' }}>
        <Table stickyHeader aria-label="Department list table" aria-busy={isLoading || isFetching}>
          <TableHead>
            <TableRow>
              {columns.map((col) => (
                <TableCell
                  key={col.id}
                  align={col.align ?? 'left'}
                  sx={{ minWidth: col.width, fontWeight: 700, bgcolor: 'background.default' }}
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
              <TableCell sx={{ width: 48, bgcolor: 'background.default' }} />
            </TableRow>
          </TableHead>

          <TableBody>
            {isLoading ? (
              <TableRow>
                <TableCell colSpan={columns.length + 1} sx={{ p: 0, border: 'none' }}>
                  <DepartmentSkeleton rows={8} columns={columns.length + 1} />
                </TableCell>
              </TableRow>
            ) : departments.length === 0 ? (
              <TableRow>
                <TableCell colSpan={columns.length + 1} sx={{ border: 'none' }}>
                  <DepartmentEmptyState
                    hasFilters={hasSearch}
                    onClear={onClearSearch}
                    onAdd={onAdd}
                    canCreate={canCreate}
                  />
                </TableCell>
              </TableRow>
            ) : (
              departments.map((dept) => (
                <TableRow
                  key={dept.id}
                  hover
                  onClick={() => onView(dept)}
                  sx={{ cursor: 'pointer' }}
                  aria-label={`Row for ${dept.name}`}
                >
                  {columns.map((col) => (
                    <TableCell key={col.id} align={col.align ?? 'left'}>
                      {col.render(dept)}
                    </TableCell>
                  ))}
                  <TableCell align="right" onClick={(e) => e.stopPropagation()}>
                    <Tooltip title="Actions">
                      <IconButton
                        size="small"
                        onClick={(e) => handleMenuOpen(e, dept)}
                        aria-label={`Actions for ${dept.name}`}
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

      <DepartmentActionsMenu
        anchorEl={menuAnchor}
        open={Boolean(menuAnchor)}
        onClose={handleMenuClose}
        onView={() => menuDept && onView(menuDept)}
        onEdit={() => menuDept && onEdit(menuDept)}
        onDelete={() => menuDept && onDelete(menuDept)}
        canEdit={canEdit}
        canDelete={canDelete}
      />
    </>
  );
}
