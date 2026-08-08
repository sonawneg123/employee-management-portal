/**
 * @fileoverview LeaveTable — sortable data table for the leave list.
 */

import React, { useState } from 'react';
import {
  IconButton, Paper, Table, TableBody, TableCell,
  TableContainer, TableHead, TableRow, TableSortLabel, Tooltip,
} from '@mui/material';
import MoreVertIcon from '@mui/icons-material/MoreVert';
import { getLeaveColumns }   from '@/utils/leaveColumns';
import LeaveActionsMenu      from './LeaveActionsMenu';
import LeaveSkeleton         from './LeaveSkeleton';
import LeaveEmptyState       from './LeaveEmptyState';
import LeaveErrorState       from './LeaveErrorState';

/**
 * @typedef {Object} LeaveTableProps
 * @property {import('@/services/leaveApi').LeaveRequestResponse[]} leaves
 * @property {boolean}   isLoading
 * @property {boolean}   isFetching
 * @property {boolean}   isError
 * @property {any}       error
 * @property {string}    sort
 * @property {'asc'|'desc'} direction
 * @property {boolean}   hasFilters
 * @property {boolean}   canApprove
 * @property {boolean}   canEdit
 * @property {boolean}   canCancel
 * @property {(field: string, dir: 'asc'|'desc') => void} onSort
 * @property {(leave: Object) => void} onView
 * @property {(leave: Object) => void} onApprove
 * @property {(leave: Object) => void} onReject
 * @property {(leave: Object) => void} onEdit
 * @property {(leave: Object) => void} onCancel
 * @property {() => void} onRetry
 * @property {() => void} onClearFilters
 * @property {() => void} [onAdd]
 * @property {boolean}   [canCreate]
 */

/**
 * Leave management data table.
 *
 * @param {LeaveTableProps} props
 * @returns {JSX.Element}
 */
export default function LeaveTable({
  leaves, isLoading, isFetching, isError, error,
  sort, direction, hasFilters,
  canApprove, canEdit, canCancel,
  onSort, onView, onApprove, onReject, onEdit, onCancel,
  onRetry, onClearFilters, onAdd, canCreate,
}) {
  const columns = getLeaveColumns();
  const [menuAnchor, setMenuAnchor] = useState(null);
  const [menuLeave,  setMenuLeave]  = useState(null);

  const handleMenuOpen  = (e, leave) => { e.stopPropagation(); setMenuAnchor(e.currentTarget); setMenuLeave(leave); };
  const handleMenuClose = () => { setMenuAnchor(null); setMenuLeave(null); };
  const handleSort      = (field) => onSort(field, field === sort ? (direction === 'asc' ? 'desc' : 'asc') : 'asc');

  if (isError) return <LeaveErrorState error={error} onRetry={onRetry} />;

  const isPending = (l) => l?.status === 'PENDING';

  return (
    <>
      <TableContainer component={Paper} variant="outlined" sx={{ borderRadius: 0, border: 'none' }}>
        <Table stickyHeader aria-label="Leave requests table" aria-busy={isLoading || isFetching}>
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
                  ) : col.label}
                </TableCell>
              ))}
              <TableCell sx={{ width: 48, bgcolor: 'background.default' }} />
            </TableRow>
          </TableHead>

          <TableBody>
            {isLoading ? (
              <TableRow>
                <TableCell colSpan={columns.length + 1} sx={{ p: 0, border: 'none' }}>
                  <LeaveSkeleton rows={8} columns={columns.length + 1} />
                </TableCell>
              </TableRow>
            ) : leaves.length === 0 ? (
              <TableRow>
                <TableCell colSpan={columns.length + 1} sx={{ border: 'none' }}>
                  <LeaveEmptyState hasFilters={hasFilters} onClear={onClearFilters} onAdd={onAdd} canCreate={canCreate} />
                </TableCell>
              </TableRow>
            ) : (
              leaves.map((leave) => (
                <TableRow
                  key={leave.id}
                  hover
                  onClick={() => onView(leave)}
                  sx={{ cursor: 'pointer' }}
                  aria-label={`Leave row for ${leave.employeeName ?? 'employee'}`}
                >
                  {columns.map((col) => (
                    <TableCell key={col.id} align={col.align ?? 'left'}>
                      {col.render(leave)}
                    </TableCell>
                  ))}
                  <TableCell align="right" onClick={(e) => e.stopPropagation()}>
                    <Tooltip title="Actions">
                      <IconButton size="small" onClick={(e) => handleMenuOpen(e, leave)}
                        aria-label={`Actions for ${leave.employeeName ?? 'leave'}`}>
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

      <LeaveActionsMenu
        anchorEl={menuAnchor}
        open={Boolean(menuAnchor)}
        onClose={handleMenuClose}
        onView={() => menuLeave && onView(menuLeave)}
        onApprove={() => menuLeave && onApprove(menuLeave)}
        onReject={() => menuLeave && onReject(menuLeave)}
        onEdit={() => menuLeave && onEdit(menuLeave)}
        onCancel={() => menuLeave && onCancel(menuLeave)}
        canApprove={canApprove && isPending(menuLeave)}
        canEdit={canEdit && isPending(menuLeave)}
        canCancel={canCancel && isPending(menuLeave)}
      />
    </>
  );
}
