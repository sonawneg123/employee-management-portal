/**
 * @fileoverview EmployeePagination — MUI-based server-side pagination controls.
 *
 * Renders rows-per-page selector and page navigation. Designed to sit below
 * the employee table and drive the parent's query params.
 */

import React from 'react';
import { TablePagination } from '@mui/material';
import {
  EMPLOYEE_PAGE_SIZE_OPTIONS,
  EMPLOYEE_DEFAULT_PAGE_SIZE,
} from '@/constants/employeeConstants';

/**
 * @typedef {Object} EmployeePaginationProps
 * @property {number}              page         - 0-based current page index.
 * @property {number}              pageSize     - Current page size.
 * @property {number}              totalElements- Total record count from API.
 * @property {(page: number) => void}     onPageChange     - Called with the new 0-based page.
 * @property {(size: number) => void}     onPageSizeChange - Called with the new page size.
 * @property {boolean}             [disabled]
 */

/**
 * Table pagination component for the employee list.
 *
 * @param {EmployeePaginationProps} props
 * @returns {JSX.Element}
 */
export default function EmployeePagination({
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
      rowsPerPageOptions={EMPLOYEE_PAGE_SIZE_OPTIONS}
      onPageChange={(_e, newPage) => onPageChange(newPage)}
      onRowsPerPageChange={(e) => {
        onPageSizeChange(parseInt(e.target.value, 10));
        onPageChange(0); // Reset to page 0 when page size changes
      }}
      disabled={disabled}
      showFirstButton
      showLastButton
      labelRowsPerPage="Rows:"
      aria-label="Employee list pagination"
      sx={{
        borderTop: '1px solid',
        borderColor: 'divider',
        '.MuiTablePagination-selectLabel': { mb: 0 },
        '.MuiTablePagination-displayedRows': { mb: 0 },
      }}
    />
  );
}
