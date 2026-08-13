/**
 * @fileoverview DepartmentSkeleton — loading placeholder for the department table.
 */

import React from 'react';
import { Box, Skeleton, Table, TableBody, TableCell, TableRow } from '@mui/material';

/**
 * @typedef {Object} DepartmentSkeletonProps
 * @property {number} [rows=8]    - Number of skeleton rows.
 * @property {number} [columns=6] - Number of columns.
 */

/**
 * Skeleton loading placeholder for the department data table.
 *
 * @param {DepartmentSkeletonProps} props
 * @returns {JSX.Element}
 */
export default function DepartmentSkeleton({ rows = 8, columns = 6 }) {
  return (
    <Table aria-busy="true" aria-label="Loading departments">
      <TableBody>
        {Array.from({ length: rows }, (_, ri) => (
          <TableRow key={ri}>
            {Array.from({ length: columns }, (_, ci) => (
              <TableCell key={ci}>
                {ci === 0 ? (
                  <Skeleton variant="circular" width={40} height={40} />
                ) : ci === 1 ? (
                  <Box>
                    <Skeleton variant="text" width="65%" />
                    <Skeleton variant="text" width="45%" />
                  </Box>
                ) : (
                  <Skeleton variant="text" width="70%" />
                )}
              </TableCell>
            ))}
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}
