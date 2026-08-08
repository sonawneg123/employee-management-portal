/**
 * @fileoverview LeaveSkeleton — loading placeholder for the leave table.
 */

import React from 'react';
import {
  Box,
  Skeleton,
  Table,
  TableBody,
  TableCell,
  TableRow,
} from '@mui/material';

/**
 * @typedef {Object} LeaveSkeletonProps
 * @property {number} [rows=8]
 * @property {number} [columns=5]
 */

/**
 * Skeleton loading placeholder for the leave data table.
 *
 * @param {LeaveSkeletonProps} props
 * @returns {JSX.Element}
 */
export default function LeaveSkeleton({ rows = 8, columns = 5 }) {
  return (
    <Table aria-busy="true" aria-label="Loading leave requests">
      <TableBody>
        {Array.from({ length: rows }, (_, ri) => (
          <TableRow key={ri}>
            {Array.from({ length: columns }, (_, ci) => (
              <TableCell key={ci}>
                {ci === 0 ? (
                  <Box>
                    <Skeleton variant="text" width="65%" />
                    <Skeleton variant="text" width="45%" />
                  </Box>
                ) : ci === 1 ? (
                  <Skeleton variant="rectangular" width={120} height={24} sx={{ borderRadius: 4 }} />
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
