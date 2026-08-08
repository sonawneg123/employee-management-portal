/**
 * @fileoverview LeaveCard — mobile card view of a single leave request.
 */

import React from 'react';
import {
  Box,
  Card,
  CardActionArea,
  CardContent,
  Chip,
  IconButton,
  Tooltip,
  Typography,
} from '@mui/material';
import MoreVertIcon      from '@mui/icons-material/MoreVert';
import WarningAmberIcon  from '@mui/icons-material/WarningAmber';
import LeaveStatusChip   from './LeaveStatusChip';
import LeaveTypeChip     from './LeaveTypeChip';
import { formatLeaveDateRange, formatLeaveWorkingDays } from '@/utils/leaveFormatters';

/**
 * @typedef {Object} LeaveCardProps
 * @property {import('@/services/leaveApi').LeaveRequestResponse} leave
 * @property {() => void}    [onClick]
 * @property {(event: React.MouseEvent, leave: Object) => void} [onMenuOpen]
 */

/**
 * Mobile-friendly leave request card.
 *
 * @param {LeaveCardProps} props
 * @returns {JSX.Element}
 */
export default function LeaveCard({ leave, onClick, onMenuOpen }) {
  return (
    <Card variant="outlined" sx={{ mb: 1.5 }} aria-label={`Leave card for ${leave.employeeName ?? 'employee'}`}>
      <Box sx={{ display: 'flex', alignItems: 'stretch' }}>
        <CardActionArea onClick={onClick} sx={{ flex: 1 }} aria-label="View leave details">
          <CardContent sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
            <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.75 }}>
                <Typography variant="subtitle2" fontWeight={700} noWrap>
                  {leave.employeeName ?? '—'}
                </Typography>
                {leave.isEmergency && (
                  <WarningAmberIcon fontSize="small" sx={{ color: 'error.main' }} />
                )}
              </Box>
              <LeaveStatusChip status={leave.status} size="small" />
            </Box>

            <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
              <LeaveTypeChip type={leave.leaveType} size="small" />
            </Box>

            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <Typography variant="caption" color="text.secondary">
                {formatLeaveDateRange(leave.startDate, leave.endDate)}
              </Typography>
              <Chip
                label={formatLeaveWorkingDays(leave.startDate, leave.endDate)}
                size="small"
                variant="filled"
                sx={{ fontSize: 11, height: 20 }}
              />
            </Box>
          </CardContent>
        </CardActionArea>

        {onMenuOpen && (
          <Box sx={{ display: 'flex', alignItems: 'center', pr: 1 }}>
            <Tooltip title="Actions">
              <IconButton size="small" onClick={(e) => onMenuOpen(e, leave)}
                aria-label="Leave actions">
                <MoreVertIcon fontSize="small" />
              </IconButton>
            </Tooltip>
          </Box>
        )}
      </Box>
    </Card>
  );
}
