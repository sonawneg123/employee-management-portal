/**
 * @fileoverview EmployeeSkeleton — loading placeholder for the employee table.
 *
 * Renders multiple skeleton rows that mirror the table layout so there is
 * minimal layout shift when real data arrives.
 */

import React from 'react';
import { Box, Skeleton, Table, TableBody, TableCell, TableRow } from '@mui/material';

/**
 * @typedef {Object} EmployeeSkeletonProps
 * @property {number} [rows=8]    - Number of skeleton rows to render.
 * @property {number} [columns=7] - Number of columns in the table.
 */

/**
 * Skeleton loading placeholder for the employee data table.
 *
 * @param {EmployeeSkeletonProps} props
 * @returns {JSX.Element}
 */
export default function EmployeeSkeleton({ rows = 8, columns = 7 }) {
  return (
    <Table aria-busy="true" aria-label="Loading employees">
      <TableBody>
        {Array.from({ length: rows }, (_, rowIdx) => (
          <TableRow key={rowIdx}>
            {Array.from({ length: columns }, (_, colIdx) => (
              <TableCell key={colIdx}>
                {colIdx === 1 ? (
                  // Employee name column — avatar + text
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
                    <Skeleton variant="circular" width={36} height={36} />
                    <Box sx={{ flex: 1 }}>
                      <Skeleton variant="text" width="70%" />
                      <Skeleton variant="text" width="50%" />
                    </Box>
                  </Box>
                ) : (
                  <Skeleton variant="text" width={colIdx === 0 ? '60%' : '80%'} />
                )}
              </TableCell>
            ))}
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}
