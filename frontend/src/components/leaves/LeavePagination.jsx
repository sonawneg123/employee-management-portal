/**
 * @fileoverview LeavePagination — server-side pagination for the leave list.
 */

import React from 'react';
import { TablePagination } from '@mui/material';
import { LEAVE_PAGE_SIZE_OPTIONS } from '@/constants/leaveConstants';

/**
 * @typedef {Object} LeavePaginationProps
 * @property {number}              page
 * @property {number}              pageSize
 * @property {number}              totalElements
 * @property {(page: number) => void}     onPageChange
 * @property {(size: number) => void}     onPageSizeChange
 * @property {boolean}             [disabled]
 */

/**
 * Table pagination for the leave list.
 *
 * @param {LeavePaginationProps} props
 * @returns {JSX.Element}
 */
export default function LeavePagination({ page, pageSize, totalElements, onPageChange, onPageSizeChange, disabled = false }) {
  return (
    <TablePagination
      component="div"
      count={totalElements}
      page={page}
      rowsPerPage={pageSize}
      rowsPerPageOptions={LEAVE_PAGE_SIZE_OPTIONS}
      onPageChange={(_e, p) => onPageChange(p)}
      onRowsPerPageChange={(e) => { onPageSizeChange(parseInt(e.target.value, 10)); onPageChange(0); }}
      disabled={disabled}
      showFirstButton
      showLastButton
      labelRowsPerPage="Rows:"
      aria-label="Leave list pagination"
      sx={{
        borderTop: '1px solid', borderColor: 'divider',
        '.MuiTablePagination-selectLabel': { mb: 0 },
        '.MuiTablePagination-displayedRows': { mb: 0 },
      }}
    />
  );
}
