/**
 * @fileoverview DepartmentPagination — server-side pagination for the department list.
 */

import React from 'react';
import { TablePagination } from '@mui/material';
import { DEPARTMENT_PAGE_SIZE_OPTIONS } from '@/constants/departmentConstants';

/**
 * @typedef {Object} DepartmentPaginationProps
 * @property {number}              page
 * @property {number}              pageSize
 * @property {number}              totalElements
 * @property {(page: number) => void}     onPageChange
 * @property {(size: number) => void}     onPageSizeChange
 * @property {boolean}             [disabled]
 */

/**
 * Table pagination for the department list.
 *
 * @param {DepartmentPaginationProps} props
 * @returns {JSX.Element}
 */
export default function DepartmentPagination({
  page,
  pageSize,
  totalElements,
  onPageChange,
  onPageSizeChange,
  disabled = false,
}) {
  return (
    <TablePagination
      component="div"
      count={totalElements}
      page={page}
      rowsPerPage={pageSize}
      rowsPerPageOptions={DEPARTMENT_PAGE_SIZE_OPTIONS}
      onPageChange={(_e, newPage) => onPageChange(newPage)}
      onRowsPerPageChange={(e) => {
        onPageSizeChange(parseInt(e.target.value, 10));
        onPageChange(0);
      }}
      disabled={disabled}
      showFirstButton
      showLastButton
      labelRowsPerPage="Rows:"
      aria-label="Department list pagination"
      sx={{
        borderTop: '1px solid',
        borderColor: 'divider',
        '.MuiTablePagination-selectLabel': { mb: 0 },
        '.MuiTablePagination-displayedRows': { mb: 0 },
      }}
    />
  );
}
