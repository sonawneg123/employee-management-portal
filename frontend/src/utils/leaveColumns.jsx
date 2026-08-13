/**
 * @fileoverview Leave table column definitions.
 *
 * Returns the column configuration array consumed by {@link LeaveTable}.
 */

import React from 'react';
import { Box, Typography } from '@mui/material';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';
import LeaveStatusChip from '@/components/leaves/LeaveStatusChip';
import LeaveTypeChip from '@/components/leaves/LeaveTypeChip';
import { formatLeaveDateRange, formatLeaveWorkingDays } from '@/utils/leaveFormatters';
import { formatDate } from '@/utils/dateUtils';
import { LEAVE_COLUMNS } from '@/constants/leaveConstants';

/**
 * @typedef {Object} LeaveTableColumn
 * @property {string}  id
 * @property {string}  label
 * @property {boolean} [sortable]
 * @property {'left'|'center'|'right'} [align]
 * @property {string}  [width]
 * @property {(row: import('@/services/leaveApi').LeaveRequestResponse) => React.ReactNode} render
 */

/**
 * Returns the full leave table column definitions.
 *
 * @returns {LeaveTableColumn[]}
 */
export function getLeaveColumns() {
  return [
    {
      id: LEAVE_COLUMNS.EMPLOYEE,
      label: 'Employee',
      sortable: false,
      width: '200px',
      render: (row) => (
        <Box>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
            <Typography variant="body2" fontWeight={600} noWrap>
              {row.employeeName ?? '—'}
            </Typography>
            {row.isEmergency && (
              <WarningAmberIcon
                fontSize="small"
                sx={{ color: 'error.main' }}
                titleAccess="Emergency leave"
              />
            )}
          </Box>
          <Typography variant="caption" color="text.secondary" noWrap>
            {row.employeeCode ?? ''} {row.departmentName ? `· ${row.departmentName}` : ''}
          </Typography>
        </Box>
      ),
    },
    {
      id: LEAVE_COLUMNS.TYPE,
      label: 'Type',
      sortable: true,
      width: '140px',
      render: (row) => <LeaveTypeChip type={row.leaveType} size="small" />,
    },
    {
      id: LEAVE_COLUMNS.START_DATE,
      label: 'Period',
      sortable: true,
      width: '200px',
      render: (row) => (
        <Box>
          <Typography variant="body2">
            {formatLeaveDateRange(row.startDate, row.endDate)}
          </Typography>
          <Typography variant="caption" color="text.secondary">
            {formatLeaveWorkingDays(row.startDate, row.endDate)}
          </Typography>
        </Box>
      ),
    },
    {
      id: LEAVE_COLUMNS.STATUS,
      label: 'Status',
      sortable: true,
      align: 'center',
      width: '120px',
      render: (row) => <LeaveStatusChip status={row.status} size="small" />,
    },
    {
      id: LEAVE_COLUMNS.SUBMITTED,
      label: 'Submitted',
      sortable: true,
      width: '120px',
      render: (row) => (
        <Typography variant="body2" color="text.secondary">
          {formatDate(row.createdAt)}
        </Typography>
      ),
    },
  ];
}
